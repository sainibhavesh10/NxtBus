package com.nxtbus.backend.repository;

import com.nxtbus.backend.entity.Stop;
import com.nxtbus.backend.repository.projection.NearbyStopView;
import com.nxtbus.backend.repository.projection.StopView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StopRepository   extends JpaRepository<Stop, String> {

    @Query(value = """
    SELECT stop_id AS stopId, stop_code AS stopCode, stop_name AS stopName,
           stop_lat AS stopLat, stop_lon AS stopLon, meters AS distanceMeters 
    FROM (
        SELECT s.*, ST_Distance(s.geom::geography, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography) AS meters
        FROM stops s
        ORDER BY s.geom <-> ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)
        LIMIT :candidatePoolSize
    ) candidates
    ORDER BY meters
    LIMIT :limitRows
    """, nativeQuery = true)
    List<NearbyStopView> findNearestStops(@Param("lat") double lat, @Param("lon") double lon,
                                          @Param("candidatePoolSize") int candidatePoolSize,
                                          @Param("limitRows") int limitRows);

    @Query(value = """
            SELECT
                stop_id   AS stopId,
                stop_code AS stopCode,
                stop_name AS stopName,
                stop_lat  AS stopLat,
                stop_lon  AS stopLon,
                zone_id as zoneId
            FROM stops
            WHERE stop_name ILIKE '%' || :query || '%'
               OR stop_name % :query
            ORDER BY
                (CASE
                    WHEN stop_name ILIKE :query               THEN 1.00
                    WHEN stop_name ILIKE :query || '%'        THEN 0.95
                    WHEN stop_name ILIKE '%' || :query || '%' THEN 0.85
                    ELSE similarity(stop_name, :query)
                 END
                 + word_similarity(:query, stop_name) * 0.01) DESC,
                char_length(stop_name) ASC,
                stop_name ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<StopView> searchByName(@Param("query") String query, @Param("limit") int limit);
}
