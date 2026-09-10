package com.nxtbus.routing.engine;

import com.nxtbus.backend.dto.realtime.EffectiveScheduleSnapshot;
import com.nxtbus.backend.dto.realtime.FootpathEdge;
import com.nxtbus.backend.dto.realtime.StopTimeEntry;
import com.nxtbus.backend.dto.realtime.TripSchedule;
import com.nxtbus.routing.index.RaptorIndex;
import com.nxtbus.routing.index.RaptorIndexBuilder;
import com.nxtbus.routing.model.Journey;
import com.nxtbus.routing.model.JourneyLeg;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class RaptorEngineTest {

    private final RaptorIndexBuilder builder = new RaptorIndexBuilder();

    // ---- fixture helpers ---------------------------------------------

    private record Stop(String id, int arr, int dep) {}

    private TripSchedule trip(String tripId, String routeId, Stop... stops) {
        List<StopTimeEntry> entries = new ArrayList<>();
        for (int i = 0; i < stops.length; i++) {
            entries.add(new StopTimeEntry(stops[i].id(), i, stops[i].arr(), stops[i].dep()));
        }
        return new TripSchedule(tripId, routeId, entries);
    }

    private RaptorIndex indexOf(List<TripSchedule> trips) {
        return indexOf(trips, Map.of());
    }

    private RaptorIndex indexOf(List<TripSchedule> trips, Map<String, List<FootpathEdge>> footpaths) {
        var snapshot = new EffectiveScheduleSnapshot(LocalDate.of(2026, 9, 9), trips, Instant.now());
        return builder.build(snapshot, footpaths);
    }

    private RaptorEngine engineFor(RaptorIndex index) {
        return new RaptorEngine(index, new boolean[index.totalTripCount()]);
    }

    // ---- tests ---------------------------------------------------------

    @Test
    void directRide_singleBoarding_producesOneLeg() {
        var t1 = trip("T1", "R1",
                new Stop("S1", 100, 100), new Stop("S2", 190, 200), new Stop("S3", 300, 300));

        var result = engineFor(indexOf(List.of(t1))).findEarliestArrival("S1", "S3", 50);

        assertThat(result).isPresent();
        Journey journey = result.get();
        assertThat(journey.arrivalTimeSeconds()).isEqualTo(300);
        assertThat(journey.legs()).hasSize(1);
        JourneyLeg leg = journey.legs().get(0);
        assertThat(leg.type()).isEqualTo(JourneyLeg.LegType.RIDE);
        assertThat(leg.fromStopId()).isEqualTo("S1");
        assertThat(leg.toStopId()).isEqualTo("S3");
        assertThat(leg.depTimeSeconds()).isEqualTo(100);
        assertThat(leg.arrTimeSeconds()).isEqualTo(300);
    }

    @Test
    void disconnectedStops_returnEmpty() {
        var t1 = trip("T1", "R1", new Stop("S1", 100, 100), new Stop("S2", 200, 200));
        var t2 = trip("T2", "R2", new Stop("S3", 300, 300), new Stop("S4", 400, 400));

        var result = engineFor(indexOf(List.of(t1, t2))).findEarliestArrival("S1", "S4", 0);

        assertThat(result).isEmpty();
    }

    @Test
    void withoutExclusion_earliestTripIsChosen() {
        var early = trip("EARLY", "R1", new Stop("S1", 100, 100), new Stop("S2", 150, 150));
        var later = trip("LATE", "R1", new Stop("S1", 200, 200), new Stop("S2", 250, 250));

        var result = engineFor(indexOf(List.of(early, later))).findEarliestArrival("S1", "S2", 50);

        assertThat(result.get().arrivalTimeSeconds()).isEqualTo(150);
        assertThat(result.get().legs().get(0).tripId()).isEqualTo("EARLY");
    }

    @Test
    void excludedTrip_isSkippedForTheNextBoardableTrip() {
        var early = trip("EARLY", "R1", new Stop("S1", 100, 100), new Stop("S2", 150, 150));
        var later = trip("LATE", "R1", new Stop("S1", 200, 200), new Stop("S2", 250, 250));
        RaptorIndex index = indexOf(List.of(early, later));

        boolean[] excluded = new boolean[index.totalTripCount()];
        excluded[index.tripPositionOf("EARLY")] = true;

        var result = new RaptorEngine(index, excluded).findEarliestArrival("S1", "S2", 50);

        assertThat(result).isPresent();
        assertThat(result.get().arrivalTimeSeconds()).isEqualTo(250);
        assertThat(result.get().legs().get(0).tripId()).isEqualTo("LATE");
    }

    @Test
    void transferAtSharedStop_acrossTwoRoutes() {
        var t1 = trip("T1", "R1", new Stop("S1", 100, 100), new Stop("S2", 150, 150));
        var t2 = trip("T2", "R2", new Stop("S2", 200, 200), new Stop("S3", 300, 300));

        var result = engineFor(indexOf(List.of(t1, t2))).findEarliestArrival("S1", "S3", 50);

        assertThat(result).isPresent();
        Journey journey = result.get();
        assertThat(journey.arrivalTimeSeconds()).isEqualTo(300);
        assertThat(journey.legs()).hasSize(2);
        assertThat(journey.legs().get(0).toStopId()).isEqualTo("S2");
        assertThat(journey.legs().get(1).fromStopId()).isEqualTo("S2");
    }

    @Test
    void transferViaFootpath_betweenTwoUnconnectedStops() {
        var t1 = trip("T1", "R1", new Stop("S1", 100, 100), new Stop("S2", 150, 150));
        var t2 = trip("T2", "R2", new Stop("S4", 230, 230), new Stop("S5", 300, 300));
        Map<String, List<FootpathEdge>> footpaths = Map.of("S2", List.of(new FootpathEdge("S4", 60)));

        var result = engineFor(indexOf(List.of(t1, t2), footpaths)).findEarliestArrival("S1", "S5", 50);

        assertThat(result).isPresent();
        Journey journey = result.get();
        assertThat(journey.arrivalTimeSeconds()).isEqualTo(300);
        assertThat(journey.legs()).hasSize(3);
        assertThat(journey.legs().get(0).type()).isEqualTo(JourneyLeg.LegType.RIDE);
        assertThat(journey.legs().get(1).type()).isEqualTo(JourneyLeg.LegType.WALK);
        assertThat(journey.legs().get(2).type()).isEqualTo(JourneyLeg.LegType.RIDE);
        assertThat(journey.legs().get(1).fromStopId()).isEqualTo("S2");
        assertThat(journey.legs().get(1).toStopId()).isEqualTo("S4");
    }

    @Test
    void tooTightFootpathConnection_missesTheOnlyTrip() {
        // Walk takes 60s (arrive S4 at 210), but R2's only trip departs S4 at 180 — already gone.
        var t1 = trip("T1", "R1", new Stop("S1", 100, 100), new Stop("S2", 150, 150));
        var t2 = trip("T2", "R2", new Stop("S4", 180, 180), new Stop("S5", 300, 300));
        Map<String, List<FootpathEdge>> footpaths = Map.of("S2", List.of(new FootpathEdge("S4", 60)));

        var result = engineFor(indexOf(List.of(t1, t2), footpaths)).findEarliestArrival("S1", "S5", 50);

        assertThat(result).isEmpty();
    }
}