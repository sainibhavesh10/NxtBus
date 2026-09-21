package com.nxtbus.backend.service.impl;

import com.nxtbus.backend.dto.RouteDto;
import com.nxtbus.backend.entity.RouteStop;
import com.nxtbus.backend.repository.RouteStopRepository;
import com.nxtbus.backend.service.RouteService;
import com.nxtbus.backend.service.RouteStopService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RouteStopServiceImpl implements RouteStopService {

    private final RouteStopRepository routeStopRepository;
    private final RouteService routeService;

    public RouteStopServiceImpl(RouteStopRepository routeStopRepository, RouteService routeService) {
        this.routeStopRepository = routeStopRepository;
        this.routeService = routeService;
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

    @Override
    @Transactional
    public RouteDto createRouteWithStops(RouteDto routeDto, List<String> stopIds) {
        if (stopIds == null || stopIds.isEmpty()) {
            throw new IllegalArgumentException("stopIds must contain at least one stop");
        }

        routeService.validateRouteDoesNotExist(routeDto.routeId());
        RouteDto savedRoute = routeService.saveRoute(routeDto);

        List<RouteStop> routeStops = new ArrayList<>();
        int stopSeq = 0;
        for (String stopId : stopIds) {
            RouteStop routeStop = new RouteStop();
            routeStop.setRouteId(savedRoute.routeId());
            routeStop.setStopSeq(stopSeq++);
            routeStop.setStopId(stopId);
            routeStops.add(routeStop);
        }

        routeStopRepository.saveAll(routeStops);

        return savedRoute;
    }
}