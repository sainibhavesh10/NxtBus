package com.nxtbus.backend.service;

import com.nxtbus.backend.dto.ShapeDto;

public interface ShapeService {
    ShapeDto getShapeByTripId(String tripId);
    ShapeDto getShapeByRouteId(String routeId);
}
