package com.nxtbus.backend.controller;

import com.nxtbus.backend.dto.RouteDto;
import com.nxtbus.backend.dto.TripDto;
import com.nxtbus.backend.response.PagedResponse;
import com.nxtbus.backend.service.RouteService;
import com.nxtbus.backend.service.TripService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RoutesController {

    private final RouteService routeService;

    private final TripService tripService;

    public RoutesController(RouteService routeService, TripService tripService){
        this.routeService = routeService;
        this.tripService = tripService;
    }

    @GetMapping("/search")
    public List<RouteDto> searchRoutes(
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int limit
    ) {
        return routeService.searchRoutes(query,limit);
    }

    @GetMapping("/{routeId}")
    public RouteDto getRoute(@PathVariable String routeId) {
        return routeService.getRouteById(routeId);
    }

    @GetMapping("{routeId}/trips")
    public PagedResponse<TripDto> getTripsByRoute(
            @RequestParam String routeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<TripDto> result = tripService.getTripsByRoute(routeId, page, size);
        return PagedResponse.from(result);
    }
}
