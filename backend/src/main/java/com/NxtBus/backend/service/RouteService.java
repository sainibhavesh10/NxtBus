package com.nxtbus.backend.service;

import com.nxtbus.backend.dto.RouteDto;
import com.nxtbus.backend.dto.StopDto;
import com.nxtbus.backend.entity.Route;
import com.nxtbus.backend.entity.Stop;
import com.nxtbus.backend.exception.RouteNotFoundException;
import com.nxtbus.backend.exception.StopNotFoundException;
import com.nxtbus.backend.repository.RouteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteService {

    private static final int MIN_SEARCH_QUERY_LENGTH = 3;
    private static final int MAX_SEARCH_RESULT_LIMIT = 50;

    private final RouteRepository routeRepository;

    public RouteService(RouteRepository routeRepository){
        this.routeRepository = routeRepository;
    }

    public List<RouteDto> searchRoutes(String query, int limit) {
        if (query == null) {
            return List.of();
        }
        String withoutSpaces = query.replaceAll("\\s+", "");
        if (withoutSpaces.length() < MIN_SEARCH_QUERY_LENGTH) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, MAX_SEARCH_RESULT_LIMIT));

        return routeRepository.searchRouteByName(query.trim(),safeLimit)
                .stream()
                .map(RouteDto::from)
                .toList();
    }

    public RouteDto getRouteById(String routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RouteNotFoundException(routeId));
        return RouteDto.from(route);
    }
}
