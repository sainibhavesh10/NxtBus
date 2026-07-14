package com.nxtbus.backend.repository;

import com.nxtbus.backend.entity.Trip;
import com.nxtbus.backend.repository.projection.TripView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, String> {

    Page<TripView> findByRouteId(String routeId, Pageable pageable);
}