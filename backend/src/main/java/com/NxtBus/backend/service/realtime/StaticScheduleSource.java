package com.nxtbus.backend.service.realtime;

import com.nxtbus.backend.dto.realtime.EffectiveScheduleSnapshot;
import com.nxtbus.backend.dto.realtime.StopTimeEntry;
import com.nxtbus.backend.dto.realtime.TripSchedule;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the effective schedule for a service date directly from the static
 * tables plus the three real-time correction tables. This replaces a
 * separately-stubbed RealtimeCancellationSource — trip_run_date,
 * trip_out_of_path and rerouted_trips together ARE the cancellation/reroute
 * logic, so there's nothing left for a separate source to do.
 *
 * Rules, all pushed down into two SQL queries rather than diffed in Java:
 *
 *   1. Base set = trips where trip_run_date.running = true for the date.
 *   2. Anything also in trip_out_of_path for that date is dropped entirely —
 *      not predictable enough to route against, excluded from both queries
 *      via NOT EXISTS.
 *   3. Anything also in rerouted_trips for that date is dropped under its
 *      real trip_id/route_id (excluded from the "normal" query) and instead
 *      emitted as a synthetic trip: tripId = routeId = "#rerouted_<tripId>_<date>",
 *      stop times taken from rerouted_trips instead of stop_times.
 *   4. Everything else keeps its real trip_id/route_id and static stop_times.
 */
@Component
public class StaticScheduleSource implements ScheduleSource {

    private final NamedParameterJdbcTemplate jdbc;

    public StaticScheduleSource(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String NORMAL_TRIPS_SQL = """
        SELECT t.trip_id, t.route_id, st.stop_id, st.stop_sequence,
               st.arrival_time, st.departure_time
        FROM trip_run_date trd
        JOIN trips t ON t.trip_id = trd.trip_id
        JOIN stop_times st ON st.trip_id = t.trip_id
        WHERE trd.date = :serviceDate
          AND trd.running = true
          AND NOT EXISTS (
              SELECT 1 FROM trip_out_of_path top
              WHERE top.trip_id = trd.trip_id AND top.service_date = trd.date
          )
          AND NOT EXISTS (
              SELECT 1 FROM rerouted_trips rr
              WHERE rr.trip_id = trd.trip_id AND rr.service_date = trd.date
          )
        ORDER BY t.trip_id, st.stop_sequence
        """;

    private static final String REROUTED_STOP_TIMES_SQL = """
        SELECT rr.trip_id, rr.stop_id, rr.stop_sequence, rr.arr_time, rr.dep_time
        FROM rerouted_trips rr
        JOIN trip_run_date trd
          ON trd.trip_id = rr.trip_id AND trd.date = rr.service_date
        WHERE rr.service_date = :serviceDate
          AND trd.running = true
          AND NOT EXISTS (
              SELECT 1 FROM trip_out_of_path top
              WHERE top.trip_id = rr.trip_id AND top.service_date = rr.service_date
          )
        ORDER BY rr.trip_id, rr.stop_sequence
        """;

    @Override
    public EffectiveScheduleSnapshot loadSnapshot(LocalDate serviceDate) {
        MapSqlParameterSource params = new MapSqlParameterSource("serviceDate", serviceDate);

        List<TripSchedule> trips = new ArrayList<>();

        List<TripSchedule> normal = new ArrayList<>();
        NormalTripGrouper normalGrouper = new NormalTripGrouper(normal);
        jdbc.query(NORMAL_TRIPS_SQL, params, normalGrouper);
        normalGrouper.flush();
        trips.addAll(normal);

        List<TripSchedule> rerouted = new ArrayList<>();
        // Passed serviceDate into the grouper
        RerouteTripGrouper rerouteGrouper = new RerouteTripGrouper(rerouted, serviceDate);
        jdbc.query(REROUTED_STOP_TIMES_SQL, params, rerouteGrouper);
        rerouteGrouper.flush();
        trips.addAll(rerouted);

        return new EffectiveScheduleSnapshot(serviceDate, trips, Instant.now());
    }

    /** Groups the ordered (trip_id, stop_sequence) rows of the "normal" query into TripSchedules. */
    private static final class NormalTripGrouper implements RowCallbackHandler {
        private final List<TripSchedule> sink;
        private String tripId;
        private String routeId;
        private List<StopTimeEntry> stops;

        NormalTripGrouper(List<TripSchedule> sink) {
            this.sink = sink;
        }

        @Override
        public void processRow(ResultSet rs) throws SQLException {
            String rowTripId = rs.getString("trip_id");
            if (!rowTripId.equals(tripId)) {
                flush();
                tripId = rowTripId;
                routeId = rs.getString("route_id");
                stops = new ArrayList<>();
            }
            stops.add(new StopTimeEntry(
                    rs.getString("stop_id"),
                    rs.getInt("stop_sequence"),
                    rs.getInt("arrival_time"),
                    rs.getInt("departure_time")
            ));
        }

        void flush() {
            if (tripId != null) {
                sink.add(new TripSchedule(tripId, routeId, stops));
            }
        }
    }

    /** Groups the ordered (trip_id, stop_sequence) rows of the reroute query and assigns synthetic ids. */
    private static final class RerouteTripGrouper implements RowCallbackHandler {
        private final List<TripSchedule> sink;
        private final LocalDate serviceDate;
        private String tripId;
        private List<StopTimeEntry> stops;

        // Accepts the LocalDate to generate the unique ID
        RerouteTripGrouper(List<TripSchedule> sink, LocalDate serviceDate) {
            this.sink = sink;
            this.serviceDate = serviceDate;
        }

        @Override
        public void processRow(ResultSet rs) throws SQLException {
            String rowTripId = rs.getString("trip_id");
            if (!rowTripId.equals(tripId)) {
                flush();
                tripId = rowTripId;
                stops = new ArrayList<>();
            }
            stops.add(new StopTimeEntry(
                    rs.getString("stop_id"),
                    rs.getInt("stop_sequence"),
                    rs.getInt("arr_time"),
                    rs.getInt("dep_time")
            ));
        }

        void flush() {
            if (tripId != null) {
                // Uses the original tripId and the serviceDate to build the new ID
                String syntheticId = "#rerouted_" + tripId + "_" + serviceDate;
                sink.add(new TripSchedule(syntheticId, syntheticId, stops));
            }
        }
    }
}