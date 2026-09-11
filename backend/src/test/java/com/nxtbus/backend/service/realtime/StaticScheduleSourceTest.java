package com.nxtbus.backend.service.realtime;

/*
 * Suggested location: src/test/java/com/nxtbus/backend/service/realtime/StaticScheduleSourceTest.java
 *
 * Test-scope dependencies needed:
 *   - com.h2database:h2
 *   - org.springframework:spring-jdbc
 *   - org.junit.jupiter:junit-jupiter
 *
 * DTO field names — all now CONFIRMED from the actual records:
 *   - TripSchedule(tripId, routeId, stopTimes)
 *   - StopTimeEntry(stopId, stopSequence, arrTimeSeconds, depTimeSeconds)
 *   - EffectiveScheduleSnapshot(serviceDate, trips, materializedAt)
 * Note the StopTimeEntry field names: arrTimeSeconds()/depTimeSeconds(),
 * not arrivalTime()/departureTime().
 *
 * Why a real H2 DB instead of mocking NamedParameterJdbcTemplate: the logic
 * under test IS the SQL (the two NOT EXISTS correlations and the join
 * conditions). Mocking the JDBC layer would only prove the Java grouping
 * code runs, not that the exclusion/priority rules actually hold.
 */

import com.nxtbus.backend.dto.realtime.EffectiveScheduleSnapshot;
import com.nxtbus.backend.dto.realtime.TripSchedule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticScheduleSourceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 9, 12);
    private static final LocalDate OTHER_DATE = LocalDate.of(2026, 9, 13);

    private EmbeddedDatabase db;
    private NamedParameterJdbcTemplate jdbc;
    private StaticScheduleSource source;

    @BeforeEach
    void setUp() {
        // Fresh, uniquely-named in-memory DB per test so tests can't bleed into each other.
        db = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName(UUID.randomUUID().toString())
                .build();
        jdbc = new NamedParameterJdbcTemplate(db);
        source = new StaticScheduleSource(jdbc);

        exec("""
            CREATE TABLE trips (
                trip_id VARCHAR(64) PRIMARY KEY,
                route_id VARCHAR(64)
            )
        """);
        exec("""
            CREATE TABLE stop_times (
                trip_id VARCHAR(64),
                stop_id VARCHAR(64),
                stop_sequence INT,
                arrival_time INT,
                departure_time INT
            )
        """);
        exec("""
            CREATE TABLE trip_run_date (
                trip_id VARCHAR(64),
                date DATE,
                running BOOLEAN
            )
        """);
        exec("""
            CREATE TABLE trip_out_of_path (
                trip_id VARCHAR(64),
                service_date DATE
            )
        """);
        exec("""
            CREATE TABLE rerouted_trips (
                trip_id VARCHAR(64),
                service_date DATE,
                stop_id VARCHAR(64),
                stop_sequence INT,
                arr_time INT,
                dep_time INT
            )
        """);
    }

    @AfterEach
    void tearDown() {
        db.shutdown();
    }

    // ---------------------------------------------------------------
    // Fixture helpers
    // ---------------------------------------------------------------

    private void exec(String ddl) {
        jdbc.getJdbcOperations().execute(ddl);
    }

    private void insertTrip(String tripId, String routeId) {
        jdbc.update("INSERT INTO trips (trip_id, route_id) VALUES (:t, :r)",
                new MapSqlParameterSource().addValue("t", tripId).addValue("r", routeId));
    }

    private void insertStopTime(String tripId, String stopId, int seq, int arr, int dep) {
        jdbc.update("""
            INSERT INTO stop_times (trip_id, stop_id, stop_sequence, arrival_time, departure_time)
            VALUES (:t, :s, :seq, :arr, :dep)
        """, new MapSqlParameterSource()
                .addValue("t", tripId).addValue("s", stopId)
                .addValue("seq", seq).addValue("arr", arr).addValue("dep", dep));
    }

    private void insertRunDate(String tripId, LocalDate date, boolean running) {
        jdbc.update("INSERT INTO trip_run_date (trip_id, date, running) VALUES (:t, :d, :r)",
                new MapSqlParameterSource()
                        .addValue("t", tripId).addValue("d", date).addValue("r", running));
    }

    private void insertOutOfPath(String tripId, LocalDate date) {
        jdbc.update("INSERT INTO trip_out_of_path (trip_id, service_date) VALUES (:t, :d)",
                new MapSqlParameterSource().addValue("t", tripId).addValue("d", date));
    }

    private void insertReroute(String tripId, LocalDate date, String stopId, int seq, int arr, int dep) {
        jdbc.update("""
            INSERT INTO rerouted_trips (trip_id, service_date, stop_id, stop_sequence, arr_time, dep_time)
            VALUES (:t, :d, :s, :seq, :arr, :dep)
        """, new MapSqlParameterSource()
                .addValue("t", tripId).addValue("d", date)
                .addValue("s", stopId).addValue("seq", seq).addValue("arr", arr).addValue("dep", dep));
    }

    private Optional<TripSchedule> find(List<TripSchedule> trips, String tripId) {
        return trips.stream().filter(t -> t.tripId().equals(tripId)).findFirst();
    }

    // ---------------------------------------------------------------
    // Rule 1 & 4: base set, real ids, static stop_times
    // ---------------------------------------------------------------

    @Test
    void normalRunningTrip_isIncludedWithRealIdsAndStaticStopTimes() {
        insertTrip("T1", "R1");
        insertStopTime("T1", "STOP_A", 1, 800, 805);
        insertStopTime("T1", "STOP_B", 2, 820, 825);
        insertRunDate("T1", DATE, true);

        EffectiveScheduleSnapshot snapshot = source.loadSnapshot(DATE);

        TripSchedule trip = find(snapshot.trips(), "T1").orElseThrow();
        assertEquals("R1", trip.routeId());
        assertEquals(2, trip.stopTimes().size());
        assertEquals("STOP_A", trip.stopTimes().get(0).stopId());
        assertEquals("STOP_B", trip.stopTimes().get(1).stopId());
    }

    @Test
    void tripMarkedNotRunning_isExcluded() {
        insertTrip("T2", "R2");
        insertStopTime("T2", "STOP_A", 1, 800, 805);
        insertRunDate("T2", DATE, false);

        EffectiveScheduleSnapshot snapshot = source.loadSnapshot(DATE);

        assertTrue(find(snapshot.trips(), "T2").isEmpty());
    }

    // ---------------------------------------------------------------
    // Rule 2: out_of_path drops the trip entirely
    // ---------------------------------------------------------------

    @Test
    void tripOutOfPath_isExcludedEntirelyEvenThoughRunning() {
        insertTrip("T3", "R3");
        insertStopTime("T3", "STOP_A", 1, 800, 805);
        insertRunDate("T3", DATE, true);
        insertOutOfPath("T3", DATE);

        EffectiveScheduleSnapshot snapshot = source.loadSnapshot(DATE);

        assertTrue(find(snapshot.trips(), "T3").isEmpty());
    }

    // ---------------------------------------------------------------
    // Rule 3: reroute emits a synthetic trip using rerouted stop times
    // ---------------------------------------------------------------

    @Test
    void reroutedTrip_emittedAsSyntheticTripUsingRerouteStopTimesNotStatic() {
        insertTrip("T4", "R4");
        insertStopTime("T4", "STOP_STATIC", 1, 100, 105); // must be ignored
        insertRunDate("T4", DATE, true);
        insertReroute("T4", DATE, "STOP_DETOUR", 1, 900, 905);

        EffectiveScheduleSnapshot snapshot = source.loadSnapshot(DATE);

        assertTrue(find(snapshot.trips(), "T4").isEmpty(), "real trip id should not appear");

        String syntheticId = "#rerouted_T4_" + DATE;
        TripSchedule synthetic = find(snapshot.trips(), syntheticId).orElseThrow();
        assertEquals(syntheticId, synthetic.routeId());
        assertEquals(1, synthetic.stopTimes().size());
        assertEquals("STOP_DETOUR", synthetic.stopTimes().get(0).stopId());
        assertEquals(900, synthetic.stopTimes().get(0).arrTimeSeconds());
    }

    // ---------------------------------------------------------------
    // The tricky interaction: out_of_path must win over rerouted_trips too
    // ---------------------------------------------------------------

    @Test
    void tripFlaggedBothOutOfPathAndRerouted_isExcludedEntirely() {
        insertTrip("T5", "R5");
        insertStopTime("T5", "STOP_A", 1, 100, 105);
        insertRunDate("T5", DATE, true);
        insertOutOfPath("T5", DATE);
        insertReroute("T5", DATE, "STOP_DETOUR", 1, 900, 905);

        EffectiveScheduleSnapshot snapshot = source.loadSnapshot(DATE);

        assertTrue(find(snapshot.trips(), "T5").isEmpty());
        assertTrue(find(snapshot.trips(), "#rerouted_T5_" + DATE).isEmpty());
    }

    @Test
    void reroutedButNotRunning_isExcluded() {
        insertTrip("T6", "R6");
        insertRunDate("T6", DATE, false);
        insertReroute("T6", DATE, "STOP_DETOUR", 1, 900, 905);

        EffectiveScheduleSnapshot snapshot = source.loadSnapshot(DATE);

        assertTrue(find(snapshot.trips(), "#rerouted_T6_" + DATE).isEmpty());
        assertEquals(0, snapshot.trips().size());
    }

    // ---------------------------------------------------------------
    // Edge case flagged in review: silent drop when stop_times is missing
    // ---------------------------------------------------------------

    @Test
    void runningTripWithNoStopTimes_isSilentlyDroppedFromResult() {
        insertTrip("T7", "R7");
        insertRunDate("T7", DATE, true);
        // no stop_times inserted at all

        EffectiveScheduleSnapshot snapshot = source.loadSnapshot(DATE);

        assertTrue(find(snapshot.trips(), "T7").isEmpty(),
                "current behavior: a running trip with no stop_times rows is dropped by the inner join, "
                        + "with no anomaly surfaced — worth confirming this is intentional");
    }

    @Test
    void tripRunningOnlyOnAnotherDate_isNotIncluded() {
        insertTrip("T8", "R8");
        insertStopTime("T8", "STOP_A", 1, 100, 105);
        insertRunDate("T8", OTHER_DATE, true);

        EffectiveScheduleSnapshot snapshot = source.loadSnapshot(DATE);

        assertTrue(find(snapshot.trips(), "T8").isEmpty());
    }

    // ---------------------------------------------------------------
    // Everything together, plus the requested total count printout
    // ---------------------------------------------------------------

    @Test
    void mixedScenario_computesEffectiveScheduleAndPrintsTotalRunningTrips() {
        // Two normal running trips
        insertTrip("N1", "R_N1");
        insertStopTime("N1", "STOP_A", 1, 100, 105);
        insertRunDate("N1", DATE, true);

        insertTrip("N2", "R_N2");
        insertStopTime("N2", "STOP_B", 1, 200, 205);
        insertRunDate("N2", DATE, true);

        // One rerouted trip
        insertTrip("RE1", "R_RE1");
        insertStopTime("RE1", "STOP_STATIC", 1, 300, 305);
        insertRunDate("RE1", DATE, true);
        insertReroute("RE1", DATE, "STOP_DETOUR", 1, 900, 905);

        // One out-of-path trip (must be excluded)
        insertTrip("OOP1", "R_OOP1");
        insertStopTime("OOP1", "STOP_C", 1, 400, 405);
        insertRunDate("OOP1", DATE, true);
        insertOutOfPath("OOP1", DATE);

        // One not-running trip (must be excluded)
        insertTrip("NR1", "R_NR1");
        insertStopTime("NR1", "STOP_D", 1, 500, 505);
        insertRunDate("NR1", DATE, false);

        EffectiveScheduleSnapshot snapshot = source.loadSnapshot(DATE);

        System.out.println("Total effective trips running on " + DATE + ": " + snapshot.trips().size());
        snapshot.trips().stream()
                .map(TripSchedule::tripId)
                .sorted()
                .forEach(id -> System.out.println("  - " + id));

        assertEquals(3, snapshot.trips().size(),
                "expected N1, N2, and the synthetic reroute of RE1; OOP1 and NR1 must be excluded");
        assertTrue(find(snapshot.trips(), "N1").isPresent());
        assertTrue(find(snapshot.trips(), "N2").isPresent());
        assertTrue(find(snapshot.trips(), "#rerouted_RE1_" + DATE).isPresent());
        assertTrue(find(snapshot.trips(), "OOP1").isEmpty());
        assertTrue(find(snapshot.trips(), "NR1").isEmpty());
    }
}