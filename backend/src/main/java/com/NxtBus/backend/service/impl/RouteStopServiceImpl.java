package com.nxtbus.backend.service.impl;

import com.nxtbus.backend.entity.RouteStop;
import com.nxtbus.backend.repository.RouteStopRepository;
import com.nxtbus.backend.service.RouteStopService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RouteStopServiceImpl implements RouteStopService {

    private final RouteStopRepository routeStopRepository;

    public RouteStopServiceImpl(RouteStopRepository routeStopRepository) {
        this.routeStopRepository = routeStopRepository;
    }

    @Override
    @Transactional
    public List<RouteStop> saveAllRouteStops(List<RouteStop> routeStops) {
        return routeStopRepository.saveAll(routeStops);
    }

    @Override
    public List<RouteStop> getRouteStopsByRouteId(String routeId) {
        return routeStopRepository.findByRouteIdOrderByStopSeqAsc(routeId);
    }
}