package com.nxtbus.backend.repository;

import com.nxtbus.backend.entity.Shape;
import com.nxtbus.backend.repository.projection.ShapeView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ShapeRepository extends JpaRepository<Shape, String> {

    @Query(value = """
            SELECT
                s.shape_id   AS shapeId,
                s.num_points AS numPoints,
                ST_AsGeoJSON(s.geom) AS geometryJson
            FROM trips t
            JOIN shapes s ON s.shape_id = t.shape_id
            WHERE t.trip_id = :tripId
            """, nativeQuery = true)
    Optional<ShapeView> findGeometryByTripId(@Param("tripId") String tripId);

    @Query(value = """
            SELECT
                s.shape_id   AS shapeId,
                s.num_points AS numPoints,
                ST_AsGeoJSON(s.geom) AS geometryJson
            FROM trips t
            JOIN shapes s ON s.shape_id = t.shape_id
            WHERE t.route_id = :routeId
            ORDER BY t.trip_id ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<ShapeView> findGeometryByRouteId(@Param("routeId") String routeId);
}