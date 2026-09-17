package com.nxtbus.backend.service.impl;

import com.nxtbus.backend.dto.RouteDto;
import com.nxtbus.backend.entity.Route;
import com.nxtbus.backend.exception.RouteNotFoundException;
import com.nxtbus.backend.repository.RouteRepository;
import com.nxtbus.backend.request.SearchRequest;
import com.nxtbus.backend.service.RouteService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteServiceImpl implements RouteService {

    private static final int MIN_SEARCH_QUERY_LENGTH = 3;
    private static final int MAX_SEARCH_RESULT_LIMIT = 50;

    private final RouteRepository routeRepository;

    public RouteServiceImpl(RouteRepository routeRepository){
        this.routeRepository = routeRepository;
    }

    @Override
    public List<RouteDto> searchRoutes(SearchRequest request) {
        int safeLimit = Math.min(request.limit(), MAX_SEARCH_RESULT_LIMIT);
        return routeRepository.searchRouteByName(request.query().trim(), safeLimit)
                .stream()
                .map(RouteDto::from)
                .toList();
    }

    @Override
    public RouteDto getRouteById(String routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RouteNotFoundException(routeId));
        return RouteDto.from(route);
    }

    @Override
    public void validateRouteExists(String routeId) {
        if (!routeRepository.existsById(routeId)) {
            throw new RouteNotFoundException(routeId);
        }
    }
}
