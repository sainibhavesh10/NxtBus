package com.nxtbus.backend.controller;

import com.nxtbus.backend.dto.RouteDto;
import com.nxtbus.backend.dto.StopSequenceDto;
import com.nxtbus.backend.dto.TripDto;
import com.nxtbus.backend.response.PagedResponse;
import com.nxtbus.backend.response.RouteStopSequenceResponse;
import com.nxtbus.backend.service.RouteService;
import com.nxtbus.backend.service.StopTimeService;
import com.nxtbus.backend.service.TripService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RoutesController {

    private final RouteService routeService;

    private final TripService tripService;

    private final StopTimeService stopTimeService;

    public RoutesController(RouteService routeService,
                            TripService tripService,
                            StopTimeService stopTimeService){
        this.routeService = routeService;
        this.tripService = tripService;
        this.stopTimeService = stopTimeService;
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
            @PathVariable String routeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<TripDto> result = tripService.getTripsByRoute(routeId, page, size);
        return PagedResponse.from(result);
    }

    @GetMapping("/{routeId}/stops")
    public RouteStopSequenceResponse getStopsByRoute(@PathVariable String routeId) {
        RouteDto routeDto = routeService.getRouteById(routeId);

        List<StopSequenceDto> stops = stopTimeService.getStopSequenceForRoute(routeId);

        return new RouteStopSequenceResponse(
                routeDto.routeId(),
                routeDto.routeShortName(),
                stops
        );
    }
}
