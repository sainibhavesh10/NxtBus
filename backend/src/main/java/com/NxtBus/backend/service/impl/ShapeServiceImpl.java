package com.nxtbus.backend.service.impl;

import com.nxtbus.backend.dto.ShapeDto;
import com.nxtbus.backend.exception.ShapeNotFoundException;
import com.nxtbus.backend.repository.ShapeRepository;
import com.nxtbus.backend.repository.projection.ShapeView;
import com.nxtbus.backend.service.RouteService;
import com.nxtbus.backend.service.ShapeService;
import com.nxtbus.backend.service.TripService;
import org.springframework.stereotype.Service;

@Service
public class ShapeServiceImpl implements ShapeService {

    private final ShapeRepository shapeRepository;
    private final TripService tripService;
    private final RouteService routeService;

    public ShapeServiceImpl(ShapeRepository shapeRepository, TripService tripService, RouteService routeService) {
        this.shapeRepository = shapeRepository;
        this.tripService = tripService;
        this.routeService = routeService;
    }

    @Override
    public ShapeDto getShapeByShapeId(String shapeId){
        ShapeView v = shapeRepository.findGeometryByShapeId(shapeId)
                .orElseThrow(() -> new ShapeNotFoundException("shapeId", shapeId));
        return ShapeDto.from(v);
    }

    @Override
    public ShapeDto getShapeByTripId(String tripId) {
        tripService.validateTripExists(tripId);
        ShapeView v = shapeRepository.findGeometryByTripId(tripId)
                .orElseThrow(() -> new ShapeNotFoundException("trip", tripId));
        return ShapeDto.from(v);
    }

    @Override
    public ShapeDto getShapeByRouteId(String routeId) {
        routeService.validateRouteExists(routeId);
        ShapeView v = shapeRepository.findGeometryByRouteId(routeId)
                .orElseThrow(() -> new ShapeNotFoundException("route", routeId));
        return ShapeDto.from(v);
    }
}
