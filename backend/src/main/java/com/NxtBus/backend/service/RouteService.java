package com.nxtbus.backend.service;

import com.nxtbus.backend.dto.RouteDto;

import java.util.List;

public interface RouteService {

    List<RouteDto> searchRoutes(String query, int limit);

    RouteDto getRouteById(String routeId);

    void validateRouteExists(String routeId);
}