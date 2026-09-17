package com.nxtbus.backend.repository;

import com.nxtbus.backend.entity.Trip;
import com.nxtbus.backend.repository.projection.TripView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripRepository extends JpaRepository<Trip, String> {

    Page<TripView> findByRouteId(String routeId, Pageable pageable);

    @Query(value = """
    SELECT EXISTS (
        SELECT 1 FROM trips WHERE route_id = :routeId
    )
    """, nativeQuery = true)
    boolean existsByRouteId(@Param("routeId") String routeId);
}