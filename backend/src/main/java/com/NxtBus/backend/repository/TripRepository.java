package com.nxtbus.backend.repository;

import com.nxtbus.backend.entity.Trip;
import com.nxtbus.backend.repository.projection.TimedStopSequenceView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
}