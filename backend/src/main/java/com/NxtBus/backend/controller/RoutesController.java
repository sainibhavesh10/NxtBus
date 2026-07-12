package com.nxtbus.backend.controller;

import com.nxtbus.backend.dto.RouteDto;
import com.nxtbus.backend.service.RouteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RoutesController {

    private final RouteService routeService;

    public RoutesController(RouteService routeService){
        this.routeService = routeService;
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
}
