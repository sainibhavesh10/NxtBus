package com.nxtbus.backend.service;

import com.nxtbus.backend.dto.ShapeDto;

public interface ShapeService {
    ShapeDto getShapeByShapeId(String shapeId);
    ShapeDto getShapeByTripId(String tripId);
    ShapeDto getShapeByRouteId(String routeId);
}
