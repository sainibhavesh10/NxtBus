package com.nxtbus.routing.index;

import com.nxtbus.backend.dto.realtime.EffectiveScheduleSnapshot;
import com.nxtbus.backend.dto.realtime.FootpathEdge;
import com.nxtbus.backend.dto.realtime.StopTimeEntry;
import com.nxtbus.backend.dto.realtime.TripSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RaptorIndexBuilder {

    private static final Logger log = LoggerFactory.getLogger(RaptorIndexBuilder.class);

    public RaptorIndex build(EffectiveScheduleSnapshot snapshot, Map<String, List<FootpathEdge>> footpaths) {

        // Group by resolved stop-sequence signature. A reroute's sequence
        // differs from its base pattern by definition, so it falls into its
        // own group here with zero special-casing.
        Map<List<String>, List<TripSchedule>> groups = new LinkedHashMap<>();
        for (TripSchedule trip : snapshot.trips()) {
            List<String> signature = trip.stopTimes().stream().map(StopTimeEntry::stopId).toList();
            groups.computeIfAbsent(signature, k -> new ArrayList<>()).add(trip);
        }

        Map<String, Integer> stopIdxById = new LinkedHashMap<>();
        for (List<String> signature : groups.keySet()) {
            for (String stopId : signature) stopIdxById.putIfAbsent(stopId, stopIdxById.size());
        }
        String[] stopIdByIdx = new String[stopIdxById.size()];
        stopIdxById.forEach((id, idx) -> stopIdByIdx[idx] = id);

        List<List<String>> routeSignatures = new ArrayList<>(groups.keySet());
        int numRoutes = routeSignatures.size();

        int[] routeStopOffsets = new int[numRoutes + 1];
        List<Integer> routeStopsList = new ArrayList<>();
        String[] displayRouteIdByRouteIdx = new String[numRoutes];

        int[] routeTripOffsets = new int[numRoutes + 1];
        int[] routeStopTimeOffsets = new int[numRoutes + 1];
        List<String> tripIdByTripPosList = new ArrayList<>();
        List<Integer> depTimesList = new ArrayList<>();
        List<Integer> arrTimesList = new ArrayList<>();
        Map<String, Integer> tripPosByTripId = new HashMap<>();

        for (int r = 0; r < numRoutes; r++) {
            List<String> signature = routeSignatures.get(r);
            List<TripSchedule> tripsInRoute = new ArrayList<>(groups.get(signature));
            tripsInRoute.sort(Comparator.comparingInt(t -> t.stopTimes().get(0).depTimeSeconds()));
            tripsInRoute = dropOvertakingTrips(tripsInRoute, signature.size());

            routeStopOffsets[r] = routeStopsList.size();
            for (String stopId : signature) routeStopsList.add(stopIdxById.get(stopId));
            displayRouteIdByRouteIdx[r] = tripsInRoute.isEmpty() ? "UNKNOWN" : tripsInRoute.get(0).routeId();

            routeTripOffsets[r] = tripIdByTripPosList.size();
            routeStopTimeOffsets[r] = depTimesList.size();

            for (TripSchedule trip : tripsInRoute) {
                if (trip.stopTimes().size() != signature.size()) {
                    throw new IllegalStateException(
                            "Trip " + trip.tripId() + " stop count doesn't match its own route signature");
                }
                int tripPos = tripIdByTripPosList.size();
                tripIdByTripPosList.add(trip.tripId());
                tripPosByTripId.put(trip.tripId(), tripPos);
                for (StopTimeEntry st : trip.stopTimes()) {
                    depTimesList.add(st.depTimeSeconds());
                    arrTimesList.add(st.arrTimeSeconds());
                }
            }
        }
        routeStopOffsets[numRoutes] = routeStopsList.size();
        routeTripOffsets[numRoutes] = tripIdByTripPosList.size();
        routeStopTimeOffsets[numRoutes] = depTimesList.size();

        // Reverse index: stop -> routes
        Map<Integer, List<int[]>> stopToRoutesTmp = new HashMap<>();
        for (int r = 0; r < numRoutes; r++) {
            List<String> signature = routeSignatures.get(r);
            for (int seq = 0; seq < signature.size(); seq++) {
                int stopIdx = stopIdxById.get(signature.get(seq));
                stopToRoutesTmp.computeIfAbsent(stopIdx, k -> new ArrayList<>()).add(new int[]{r, seq});
            }
        }
        int[] stopRouteOffsets = new int[stopIdByIdx.length + 1];
        List<Integer> stopRoutesList = new ArrayList<>();
        List<Integer> stopRouteSeqList = new ArrayList<>();
        for (int s = 0; s < stopIdByIdx.length; s++) {
            stopRouteOffsets[s] = stopRoutesList.size();
            for (int[] pair : stopToRoutesTmp.getOrDefault(s, List.of())) {
                stopRoutesList.add(pair[0]);
                stopRouteSeqList.add(pair[1]);
            }
        }
        stopRouteOffsets[stopIdByIdx.length] = stopRoutesList.size();

        // Footpaths -- only between stops actually served today.
        int[] footpathOffsets = new int[stopIdByIdx.length + 1];
        List<Integer> footpathTargetsList = new ArrayList<>();
        List<Integer> footpathDurationsList = new ArrayList<>();
        for (int s = 0; s < stopIdByIdx.length; s++) {
            footpathOffsets[s] = footpathTargetsList.size();
            for (FootpathEdge fp : footpaths.getOrDefault(stopIdByIdx[s], List.of())) {
                Integer targetIdx = stopIdxById.get(fp.targetStopId());
                if (targetIdx == null) continue; // target has no active trips today -- dead end, skip
                footpathTargetsList.add(targetIdx);
                footpathDurationsList.add(fp.durationSeconds());
            }
        }
        footpathOffsets[stopIdByIdx.length] = footpathTargetsList.size();

        return new RaptorIndex(
                stopIdByIdx, stopIdxById,
                routeStopOffsets, toIntArray(routeStopsList), displayRouteIdByRouteIdx,
                routeTripOffsets, tripIdByTripPosList.toArray(new String[0]), tripPosByTripId,
                routeStopTimeOffsets, toIntArray(depTimesList), toIntArray(arrTimesList),
                stopRouteOffsets, toIntArray(stopRoutesList), toIntArray(stopRouteSeqList),
                footpathOffsets, toIntArray(footpathTargetsList), toIntArray(footpathDurationsList)
        );
    }

    /** A route's trips must never overtake each other, or binary-search
     *  boarding silently breaks. This is a data problem (bad times), not
     *  something to paper over -- drop the offending trip and log it. */
    private List<TripSchedule> dropOvertakingTrips(List<TripSchedule> sortedTrips, int numStops) {
        List<TripSchedule> valid = new ArrayList<>();
        int[] lastDepAtStop = new int[numStops];
        Arrays.fill(lastDepAtStop, Integer.MIN_VALUE);

        for (TripSchedule trip : sortedTrips) {
            boolean overtakes = false;
            for (int i = 0; i < numStops; i++) {
                if (trip.stopTimes().get(i).depTimeSeconds() < lastDepAtStop[i]) { overtakes = true; break; }
            }
            if (overtakes) {
                log.warn("Dropping trip {} -- overtakes an earlier trip on the same pattern", trip.tripId());
                continue;
            }
            for (int i = 0; i < numStops; i++) lastDepAtStop[i] = trip.stopTimes().get(i).depTimeSeconds();
            valid.add(trip);
        }
        return valid;
    }

    private int[] toIntArray(List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = list.get(i);
        return arr;
    }
}