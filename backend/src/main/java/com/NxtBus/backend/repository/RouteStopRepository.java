package com.nxtbus.backend.repository;

import com.nxtbus.backend.entity.RouteStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteStopRepository extends JpaRepository<RouteStop, RouteStop.RouteStopId> {

    List<RouteStop> findByRouteIdOrderByStopSeqAsc(String routeId);
}