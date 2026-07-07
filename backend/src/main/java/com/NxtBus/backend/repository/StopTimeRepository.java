package com.nxtbus.backend.repository;

import com.nxtbus.backend.entity.StopTime;
import com.nxtbus.backend.repository.projection.UpcomingDeparture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StopTimeRepository extends JpaRepository<StopTime, StopTime.StopTimeId> {

    @Query(value = """
        SELECT st.trip_id AS tripId, st.departure_time AS departureTime,
               t.route_id AS routeId, r.route_short_name AS routeShortName, r.route_long_name AS routeLongName
        FROM gtfs_stop_times st
        JOIN gtfs_trips t  ON t.trip_id = st.trip_id
        JOIN gtfs_routes r ON r.route_id = t.route_id
        JOIN trip_run_date trd ON trd.trip_id = st.trip_id AND trd.date = CURRENT_DATE
        WHERE st.stop_id = :stopId AND trd.running = TRUE AND st.departure_time >= :afterSeconds
        ORDER BY st.departure_time
        LIMIT :limitRows
        """, nativeQuery = true)
    List<UpcomingDeparture> findUpcomingDepartures(
            @Param("stopId") String stopId,
            @Param("afterSeconds") int afterSeconds,
            @Param("limitRows") int limitRows
    );

    @Query(value = """
        (
          SELECT st.trip_id AS tripId, st.departure_time AS departureTime,
                 st.departure_time AS sortKey,
                 t.route_id AS routeId, r.route_short_name AS routeShortName, r.route_long_name AS routeLongName
          FROM gtfs_stop_times st
          JOIN gtfs_trips t  ON t.trip_id = st.trip_id
          JOIN gtfs_routes r ON r.route_id = t.route_id
          JOIN trip_run_date trd ON trd.trip_id = st.trip_id AND trd.date = CURRENT_DATE
          WHERE st.stop_id = :stopId AND trd.running = TRUE AND st.departure_time >= :afterSeconds
        )
        UNION ALL
        (
          SELECT st.trip_id, st.departure_time,
                 st.departure_time - 86400 AS sortKey,
                 t.route_id, r.route_short_name, r.route_long_name
          FROM gtfs_stop_times st
          JOIN gtfs_trips t  ON t.trip_id = st.trip_id
          JOIN gtfs_routes r ON r.route_id = t.route_id
          JOIN trip_run_date trd ON trd.trip_id = st.trip_id AND trd.date = CURRENT_DATE - 1
          WHERE st.stop_id = :stopId AND trd.running = TRUE AND st.departure_time >= :afterSeconds + 86400
        )
        ORDER BY sortKey
        LIMIT :limitRows
        """, nativeQuery = true)
    List<UpcomingDeparture> findUpcomingDeparturesIncludingOvernight(
            @Param("stopId") String stopId,
            @Param("afterSeconds") int afterSeconds,
            @Param("limitRows") int limitRows
    );
}
