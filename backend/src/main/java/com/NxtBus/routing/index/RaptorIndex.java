package com.nxtbus.routing.index;

import java.util.Map;

/**
 * Immutable, array-based RAPTOR structure for ONE service date.
 * Never mutated after construction. All internal indices are dense ints
 * assigned during THIS build only — never persist or compare them across
 * two different RaptorIndex instances.
 */
public final class RaptorIndex {

    private final String[] stopIdByIdx;
    private final Map<String, Integer> stopIdxById;

    private final int[] routeStopOffsets;          // CSR: route -> stops
    private final int[] routeStops;
    private final String[] displayRouteIdByRouteIdx; // human-readable, UI only

    private final int[] routeTripOffsets;          // CSR: route -> trip IDENTITY (size totalTrips)
    private final String[] tripIdByTripPos;
    private final Map<String, Integer> tripPosByTripId;

    private final int[] routeStopTimeOffsets;      // CSR: route -> stop-time DATA BLOCK (numTrips*numStops)
    private final int[] depTimes;
    private final int[] arrTimes;

    private final int[] stopRouteOffsets;          // CSR: stop -> routes
    private final int[] stopRoutes;
    private final int[] stopRouteSeq;              // this stop's position within that route

    private final int[] footpathOffsets;           // CSR: stop -> nearby stops
    private final int[] footpathTargets;
    private final int[] footpathDurations;

    RaptorIndex(
            String[] stopIdByIdx, Map<String, Integer> stopIdxById,
            int[] routeStopOffsets, int[] routeStops, String[] displayRouteIdByRouteIdx,
            int[] routeTripOffsets, String[] tripIdByTripPos, Map<String, Integer> tripPosByTripId,
            int[] routeStopTimeOffsets, int[] depTimes, int[] arrTimes,
            int[] stopRouteOffsets, int[] stopRoutes, int[] stopRouteSeq,
            int[] footpathOffsets, int[] footpathTargets, int[] footpathDurations
    ) {
        this.stopIdByIdx = stopIdByIdx;
        this.stopIdxById = stopIdxById;
        this.routeStopOffsets = routeStopOffsets;
        this.routeStops = routeStops;
        this.displayRouteIdByRouteIdx = displayRouteIdByRouteIdx;
        this.routeTripOffsets = routeTripOffsets;
        this.tripIdByTripPos = tripIdByTripPos;
        this.tripPosByTripId = tripPosByTripId;
        this.routeStopTimeOffsets = routeStopTimeOffsets;
        this.depTimes = depTimes;
        this.arrTimes = arrTimes;
        this.stopRouteOffsets = stopRouteOffsets;
        this.stopRoutes = stopRoutes;
        this.stopRouteSeq = stopRouteSeq;
        this.footpathOffsets = footpathOffsets;
        this.footpathTargets = footpathTargets;
        this.footpathDurations = footpathDurations;
    }

    public int numStops() { return stopIdByIdx.length; }
    public int numRoutes() { return routeStopOffsets.length - 1; }
    public int totalTripCount() { return tripIdByTripPos.length; }

    public int stopIdx(String stopId) {
        Integer idx = stopIdxById.get(stopId);
        return idx == null ? -1 : idx;
    }
    public String stopId(int stopIdx) { return stopIdByIdx[stopIdx]; }

    public Integer tripPositionOf(String tripId) { return tripPosByTripId.get(tripId); }
    public String tripId(int tripPos) { return tripIdByTripPos[tripPos]; }
    public String displayRouteId(int routeIdx) { return displayRouteIdByRouteIdx[routeIdx]; }

    public int numStopsInRoute(int routeIdx) {
        return routeStopOffsets[routeIdx + 1] - routeStopOffsets[routeIdx];
    }
    public int stopInRoute(int routeIdx, int posInRoute) {
        return routeStops[routeStopOffsets[routeIdx] + posInRoute];
    }

    public int numTripsInRoute(int routeIdx) {
        return routeTripOffsets[routeIdx + 1] - routeTripOffsets[routeIdx];
    }
    /** Global trip position for the tripIdxInRoute-th trip of this route (0-based, sorted by departure). */
    public int tripPos(int routeIdx, int tripIdxInRoute) {
        return routeTripOffsets[routeIdx] + tripIdxInRoute;
    }

    private int stopTimeSlot(int routeIdx, int tripIdxInRoute, int stopPosInRoute) {
        int numStops = numStopsInRoute(routeIdx);
        return routeStopTimeOffsets[routeIdx] + tripIdxInRoute * numStops + stopPosInRoute;
    }
    public int depTime(int routeIdx, int tripIdxInRoute, int stopPosInRoute) {
        return depTimes[stopTimeSlot(routeIdx, tripIdxInRoute, stopPosInRoute)];
    }
    public int arrTime(int routeIdx, int tripIdxInRoute, int stopPosInRoute) {
        return arrTimes[stopTimeSlot(routeIdx, tripIdxInRoute, stopPosInRoute)];
    }

    public int routesAtStopStart(int stopIdx) { return stopRouteOffsets[stopIdx]; }
    public int routesAtStopEnd(int stopIdx) { return stopRouteOffsets[stopIdx + 1]; }
    public int routeAt(int pos) { return stopRoutes[pos]; }
    public int routeSeqAt(int pos) { return stopRouteSeq[pos]; }

    public int footpathsStart(int stopIdx) { return footpathOffsets[stopIdx]; }
    public int footpathsEnd(int stopIdx) { return footpathOffsets[stopIdx + 1]; }
    public int footpathTarget(int pos) { return footpathTargets[pos]; }
    public int footpathDuration(int pos) { return footpathDurations[pos]; }
}