package com.nxtbus.backend.service.impl;

import com.nxtbus.backend.dto.ShapeDto;
import com.nxtbus.backend.exception.ShapeNotFoundException;
import com.nxtbus.backend.repository.ShapeRepository;
import com.nxtbus.backend.repository.projection.ShapeView;
import com.nxtbus.backend.service.ShapeService;
import org.springframework.stereotype.Service;

@Service
public class ShapeServiceImpl implements ShapeService {

    private final ShapeRepository shapeRepository;

    public ShapeServiceImpl(ShapeRepository shapeRepository) {
        this.shapeRepository = shapeRepository;
    }

    @Override
    public ShapeDto getShapeByTripId(String tripId) {
        ShapeView v = shapeRepository.findGeometryByTripId(tripId)
                .orElseThrow(() -> new ShapeNotFoundException(tripId));
        return ShapeDto.from(v);
    }

    @Override
    public ShapeDto getShapeByRouteId(String routeId) {
        ShapeView v = shapeRepository.findGeometryByRouteId(routeId)
                .orElseThrow(() -> new ShapeNotFoundException(routeId));
        return ShapeDto.from(v);
    }
}
