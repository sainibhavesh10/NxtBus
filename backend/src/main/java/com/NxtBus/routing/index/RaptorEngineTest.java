package com.nxtbus.routing.index;

import com.nxtbus.routing.engine.RaptorEngine;
import com.nxtbus.routing.model.Journey;
import com.nxtbus.routing.model.JourneyLeg;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Hand-built fixture -- no Spring context, no DB, no RaptorIndexBuilder.
 * Deliberately lives in com.nxtbus.routing.index so it can call RaptorIndex's
 * package-private constructor directly and pin exact array contents.
 *
 * Topology:
 *
 *   Route A (2 trips): S1 -> S2
 *   Route B (2 trips): S3 -> S4
 *   Footpath: S2 --(50s)--> S3
 *
 *   Trip A1: dep S1 100 / arr S2 200
 *   Trip A2: dep S1 300 / arr S2 400
 *   Trip B1: dep S3 260 / arr S4 350
 *   Trip B2: dep S3 500 / arr S4 600
 *
 * With nothing excluded, S1->S4 departing at t=0 rides A1, walks the
 * footpath, and just catches B1 (arrive S3 @250, B1 dep 260) -> arrival 350.
 * Excluding A1 pushes the rider onto A2 (arr S2 @400), walking in @450 --
 * too late for B1 -- so the plan falls back to B2, arriving @600.
 */
class RaptorEngineTest {

    private static final int S1 = 0, S2 = 1, S3 = 2, S4 = 3;
    private static final int ROUTE_A = 0, ROUTE_B = 1;

    private RaptorIndex buildFixtureIndex() {
        String[] stopIdByIdx = {"S1", "S2", "S3", "S4"};
        Map<String, Integer> stopIdxById = new LinkedHashMap<>();
        stopIdxById.put("S1", S1);
        stopIdxById.put("S2", S2);
        stopIdxById.put("S3", S3);
        stopIdxById.put("S4", S4);

        // Route -> stops (CSR)
        int[] routeStopOffsets = {0, 2, 4};
        int[] routeStops = {S1, S2, S3, S4};
        String[] displayRouteIdByRouteIdx = {"A", "B"};

        // Route -> trip identity (CSR). Global trip positions: A1=0, A2=1, B1=2, B2=3
        int[] routeTripOffsets = {0, 2, 4};
        String[] tripIdByTripPos = {"A1", "A2", "B1", "B2"};
        Map<String, Integer> tripPosByTripId = new LinkedHashMap<>();
        tripPosByTripId.put("A1", 0);
        tripPosByTripId.put("A2", 1);
        tripPosByTripId.put("B1", 2);
        tripPosByTripId.put("B2", 3);

        // Route -> stop-time data block (numTrips * numStops per route -- separate
        // offset array from routeTripOffsets, per the CSR note from the index design).
        int[] routeStopTimeOffsets = {0, 4, 8};
        //                       A1@S1 A1@S2 A2@S1 A2@S2   B1@S3 B1@S4 B2@S3 B2@S4
        int[] depTimes = {        100,  200,  300,  400,     260,  350,  500,  600 };
        int[] arrTimes = {        100,  200,  300,  400,     260,  350,  500,  600 };

        // Stop -> routes serving it (CSR), with this stop's seq position within that route
        int[] stopRouteOffsets = {0, 1, 2, 3, 4};
        int[] stopRoutes   = {ROUTE_A, ROUTE_A, ROUTE_B, ROUTE_B}; // for S1, S2, S3, S4
        int[] stopRouteSeq = {0,       1,       0,       1};

        // Footpaths: only S2 -> S3, 50s. No reverse edge -- matches builder's
        // per-direction footpath handling.
        int[] footpathOffsets = {0, 0, 1, 1, 1};
        int[] footpathTargets = {S3};
        int[] footpathDurations = {50};

        return new RaptorIndex(
                stopIdByIdx, stopIdxById,
                routeStopOffsets, routeStops, displayRouteIdByRouteIdx,
                routeTripOffsets, tripIdByTripPos, tripPosByTripId,
                routeStopTimeOffsets, depTimes, arrTimes,
                stopRouteOffsets, stopRoutes, stopRouteSeq,
                footpathOffsets, footpathTargets, footpathDurations
        );
    }

    @Test
    void earliestArrival_withNoExclusions_ridesFirstTripThenWalksToSecondRoute() {
        RaptorIndex index = buildFixtureIndex();
        boolean[] excluded = new boolean[index.totalTripCount()]; // nobody excluded

        RaptorEngine engine = new RaptorEngine(index, excluded);
        Optional<Journey> result = engine.findEarliestArrival("S1", "S4", 0);

        assertTrue(result.isPresent());
        Journey journey = result.get();
        assertEquals(350, journey.arrivalTimeSeconds());
        assertEquals(3, journey.legs().size());

        JourneyLeg ride1 = journey.legs().get(0);
        assertEquals(JourneyLeg.LegType.RIDE, ride1.type());
        assertEquals("A1", ride1.tripId());
        assertEquals("S1", ride1.fromStopId());
        assertEquals("S2", ride1.toStopId());
        assertEquals(100, ride1.depTimeSeconds());
        assertEquals(200, ride1.arrTimeSeconds());

        JourneyLeg walk = journey.legs().get(1);
        assertEquals(JourneyLeg.LegType.WALK, walk.type());
        assertEquals("S2", walk.fromStopId());
        assertEquals("S3", walk.toStopId());

        JourneyLeg ride2 = journey.legs().get(2);
        assertEquals(JourneyLeg.LegType.RIDE, ride2.type());
        assertEquals("B1", ride2.tripId());
        assertEquals("S3", ride2.fromStopId());
        assertEquals("S4", ride2.toStopId());
        assertEquals(260, ride2.depTimeSeconds());
        assertEquals(350, ride2.arrTimeSeconds());
    }

    @Test
    void earliestArrival_withFirstTripExcluded_boardsNextTripAndMissesTheConnection() {
        RaptorIndex index = buildFixtureIndex();
        boolean[] excluded = new boolean[index.totalTripCount()];
        excluded[index.tripPositionOf("A1")] = true; // e.g. a cancelled trip

        RaptorEngine engine = new RaptorEngine(index, excluded);
        Optional<Journey> result = engine.findEarliestArrival("S1", "S4", 0);

        assertTrue(result.isPresent());
        Journey journey = result.get();
        assertEquals(600, journey.arrivalTimeSeconds(),
                "missing B1's window should force a fallback to B2, not a partial/blocked route");

        JourneyLeg firstLeg = journey.legs().get(0);
        assertEquals("A2", firstLeg.tripId(),
                "excluded A1 should be skipped for the next boardable trip on the same route");

        JourneyLeg lastLeg = journey.legs().get(journey.legs().size() - 1);
        assertEquals("B2", lastLeg.tripId(),
                "arriving late at S3 should miss B1's departure and fall back to B2");
    }

    @Test
    void earliestArrival_withEntireConnectingRouteExcluded_isUnreachable() {
        RaptorIndex index = buildFixtureIndex();
        boolean[] excluded = new boolean[index.totalTripCount()];
        excluded[index.tripPositionOf("B1")] = true;
        excluded[index.tripPositionOf("B2")] = true;

        RaptorEngine engine = new RaptorEngine(index, excluded);

        assertTrue(engine.findEarliestArrival("S1", "S4", 0).isEmpty(),
                "no boardable trip on the only connecting route should mean no journey, not a partial one");
    }

    @Test
    void earliestArrival_withUnknownStopId_returnsEmpty() {
        RaptorIndex index = buildFixtureIndex();
        boolean[] excluded = new boolean[index.totalTripCount()];

        RaptorEngine engine = new RaptorEngine(index, excluded);

        assertTrue(engine.findEarliestArrival("S1", "S99-does-not-exist", 0).isEmpty());
    }
}