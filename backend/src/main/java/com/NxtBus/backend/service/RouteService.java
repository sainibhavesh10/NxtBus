package com.nxtbus.backend.service;

import com.nxtbus.backend.dto.RouteDto;
import com.nxtbus.backend.request.SearchRequest;

import java.util.List;

public interface RouteService {

    List<RouteDto> searchRoutes(SearchRequest request);

    RouteDto getRouteById(String routeId);

    void validateRouteExists(String routeId);
}