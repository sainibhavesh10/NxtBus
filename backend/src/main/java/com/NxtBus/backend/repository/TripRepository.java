package com.nxtbus.backend.repository;

import com.nxtbus.backend.entity.Trip;
import com.nxtbus.backend.repository.projection.TimedStopSequenceView;
import com.nxtbus.backend.repository.projection.TripShapeView;
import com.nxtbus.backend.repository.projection.TripView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, String> {

    @Query(value = """
        SELECT
            st.stop_sequence  AS stopSequence,
            s.stop_id         AS stopId,
            s.stop_name       AS stopName,
            s.stop_code       AS stopCode,
            st.arrival_time   AS arrivalTime,
            st.departure_time AS departureTime,
            s.stop_lat        AS lat,
            s.stop_lon        AS lon
        FROM stop_times st
        JOIN stops s ON s.stop_id = st.stop_id
        WHERE st.trip_id = :tripId
        ORDER BY st.stop_sequence
        """, nativeQuery = true)
    List<TimedStopSequenceView> findStopsByTripId(@Param("tripId") String tripId);

    @Query(value = """
    SELECT
        t.trip_id       AS tripId,
        t.route_id      AS routeId,
        t.service_id    AS serviceId,
        t.shape_id      AS shapeId,
        t.trip_headsign AS tripHeadsign,
        t.direction_id  AS directionId,
        t.block_id      AS blockId,
        ST_AsGeoJSON(s.geom) AS geometryJson
    FROM trips t
    JOIN shapes s ON s.shape_id = t.shape_id
    WHERE t.trip_id = :tripId
    """, nativeQuery = true)
    Optional<TripShapeView> findShapeByTripId(@Param("tripId") String tripId);

    Page<TripView> findByRouteId(String routeId, Pageable pageable);
}