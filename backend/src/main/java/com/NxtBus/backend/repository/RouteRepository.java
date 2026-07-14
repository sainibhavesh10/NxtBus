package com.nxtbus.backend.repository;

import com.nxtbus.backend.entity.Route;
import com.nxtbus.backend.repository.projection.RouteView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RouteRepository  extends JpaRepository<Route, String> {

    @Query(value = """
        SELECT
            r.route_id         AS routeId,
            r.agency_id        AS agencyId,
            r.route_short_name AS routeShortName,
            r.route_long_name  AS routeLongName,
            r.route_type       AS routeType,
            r.route_color      AS routeColor,
            r.route_text_color AS routeTextColor
        FROM routes r
        WHERE r.route_short_name ILIKE '%' || :query || '%'
           OR r.route_long_name ILIKE '%' || :query || '%'
        ORDER BY
          CASE
            WHEN r.route_short_name ILIKE :query THEN 1
            WHEN r.route_short_name ILIKE :query || '%' THEN 2
            WHEN r.route_short_name ILIKE '%' || :query || '%' THEN 3
            WHEN r.route_long_name ILIKE '%' || :query || '%' THEN 4
            ELSE 5
          END,
          r.route_short_name
        LIMIT :limit
        """, nativeQuery = true)
    List<RouteView> searchRouteByName(@Param("query") String query, @Param("limit") int limit);
}
