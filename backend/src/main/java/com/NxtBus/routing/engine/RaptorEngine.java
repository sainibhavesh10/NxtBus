package com.nxtbus.routing.engine;

import com.nxtbus.routing.index.RaptorIndex;
import com.nxtbus.routing.model.Journey;
import com.nxtbus.routing.model.JourneyLeg;

import java.util.*;

public class RaptorEngine {

    private static final int UNREACHABLE = Integer.MAX_VALUE;
    private static final int MAX_ROUNDS = 6; // at most ~5 transfers

    private final RaptorIndex index;
    private final boolean[] excludedTrips;
    private final int[] earliestArrival;
    private final ParentLabel[] parent;

    public RaptorEngine(RaptorIndex index, boolean[] excludedTrips) {
        this.index = index;
        this.excludedTrips = excludedTrips;
        this.earliestArrival = new int[index.numStops()];
        Arrays.fill(earliestArrival, UNREACHABLE);
        this.parent = new ParentLabel[index.numStops()];
    }

    public Optional<Journey> findEarliestArrival(String fromStopId, String toStopId, int departTimeSeconds) {
        int source = index.stopIdx(fromStopId);
        int target = index.stopIdx(toStopId);
        if (source < 0 || target < 0) return Optional.empty();

        earliestArrival[source] = departTimeSeconds;
        Set<Integer> marked = new HashSet<>(Set.of(source));
        relaxFootpaths(marked);

        for (int round = 0; round < MAX_ROUNDS && !marked.isEmpty(); round++) {
            Map<Integer, Integer> routesToScan = collectRoutes(marked);
            marked.clear();
            for (Map.Entry<Integer, Integer> e : routesToScan.entrySet()) {
                scanRoute(e.getKey(), e.getValue(), marked);
            }
            relaxFootpaths(marked);
        }

        if (earliestArrival[target] == UNREACHABLE) return Optional.empty();
        return Optional.of(reconstructJourney(source, target, departTimeSeconds));
    }

    private Map<Integer, Integer> collectRoutes(Set<Integer> markedStops) {
        Map<Integer, Integer> routeToEarliestPos = new HashMap<>();
        for (int stop : markedStops) {
            for (int p = index.routesAtStopStart(stop); p < index.routesAtStopEnd(stop); p++) {
                routeToEarliestPos.merge(index.routeAt(p), index.routeSeqAt(p), Math::min);
            }
        }
        return routeToEarliestPos;
    }

    private void scanRoute(int route, int startPos, Set<Integer> markedStops) {
        int numStops = index.numStopsInRoute(route);
        int boardedTrip = -1, boardingPos = -1;

        for (int pos = startPos; pos < numStops; pos++) {
            int stop = index.stopInRoute(route, pos);

            if (boardedTrip >= 0) {
                int arr = index.arrTime(route, boardedTrip, pos);
                if (arr < earliestArrival[stop]) {
                    earliestArrival[stop] = arr;
                    parent[stop] = ParentLabel.ride(route, boardedTrip, boardingPos, pos);
                    markedStops.add(stop);
                }
            }

            int readyTime = earliestArrival[stop];
            if (readyTime != UNREACHABLE) {
                int earlierTrip = findEarliestBoardableTrip(route, pos, readyTime);
                if (earlierTrip >= 0 && (boardedTrip < 0 || earlierTrip < boardedTrip)) {
                    boardedTrip = earlierTrip;
                    boardingPos = pos;
                }
            }
        }
    }

    /** Binary search, then scan forward past excluded trips -- bounded by
     *  how many consecutive trips on ONE route are excluded, not dataset size. */
    private int findEarliestBoardableTrip(int route, int posInRoute, int readyTime) {
        int numTrips = index.numTripsInRoute(route);
        int lo = 0, hi = numTrips;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (index.depTime(route, mid, posInRoute) >= readyTime) hi = mid; else lo = mid + 1;
        }
        for (int t = lo; t < numTrips; t++) {
            if (!excludedTrips[index.tripPos(route, t)]) return t;
        }
        return -1;
    }

    private void relaxFootpaths(Set<Integer> newlyMarked) {
        List<Integer> toAdd = new ArrayList<>();
        for (int stop : newlyMarked) {
            int fromArrival = earliestArrival[stop];
            for (int p = index.footpathsStart(stop); p < index.footpathsEnd(stop); p++) {
                int target = index.footpathTarget(p);
                int candidate = fromArrival + index.footpathDuration(p);
                if (candidate < earliestArrival[target]) {
                    earliestArrival[target] = candidate;
                    parent[target] = ParentLabel.walk(stop);
                    toAdd.add(target);
                }
            }
        }
        newlyMarked.addAll(toAdd);
    }

    private Journey reconstructJourney(int source, int target, int departTime) {
        LinkedList<JourneyLeg> legs = new LinkedList<>();
        int stop = target;
        while (stop != source && parent[stop] != null) {
            ParentLabel label = parent[stop];
            if (label.isWalk()) {
                legs.addFirst(JourneyLeg.walk(index.stopId(label.fromStop()), index.stopId(stop)));
                stop = label.fromStop();
            } else {
                int boardStop = index.stopInRoute(label.route(), label.boardPos());
                String tripId = index.tripId(index.tripPos(label.route(), label.tripIdxInRoute()));
                legs.addFirst(JourneyLeg.ride(
                        index.displayRouteId(label.route()), tripId,
                        index.stopId(boardStop), index.stopId(stop),
                        index.depTime(label.route(), label.tripIdxInRoute(), label.boardPos()),
                        index.arrTime(label.route(), label.tripIdxInRoute(), label.alightPos())
                ));
                stop = boardStop;
            }
        }
        return new Journey(departTime, earliestArrival[target], legs);
    }

    private record ParentLabel(int route, int tripIdxInRoute, int boardPos, int alightPos, int fromStop, boolean walk) {
        static ParentLabel ride(int route, int trip, int boardPos, int alightPos) {
            return new ParentLabel(route, trip, boardPos, alightPos, -1, false);
        }
        static ParentLabel walk(int fromStop) {
            return new ParentLabel(-1, -1, -1, -1, fromStop, true);
        }
        boolean isWalk() { return walk; }
    }
}