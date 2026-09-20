package com.nxtbus.backend.controller;

import com.nxtbus.backend.dto.*;
import com.nxtbus.backend.request.NewRouteRequest;
import com.nxtbus.backend.request.PageRequestDto;
import com.nxtbus.backend.request.SearchRequest;
import com.nxtbus.backend.response.GeoJsonFeatureResponse;
import com.nxtbus.backend.response.PagedResponse;
import com.nxtbus.backend.response.RouteStopSequenceResponse;
import com.nxtbus.backend.service.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RoutesController {

    private final RouteService routeService;

    private final TripService tripService;

    private final StopTimeService stopTimeService;

    private final ShapeService shapeService;

    private final RouteStopService routeStopService;

    public RoutesController(RouteService routeService,
                            TripService tripService,
                            StopTimeService stopTimeService,
                            ShapeService shapeService,
                            RouteStopService routeStopService){
        this.routeService = routeService;
        this.tripService = tripService;
        this.stopTimeService = stopTimeService;
        this.shapeService = shapeService;
        this.routeStopService = routeStopService;
    }

    @GetMapping("/search")
    public List<RouteDto> searchRoutes(@Valid @ModelAttribute SearchRequest request) {
        return routeService.searchRoutes(request);
    }

    @GetMapping("/{routeId}")
    public RouteDto getRoute(@PathVariable String routeId) {
        return routeService.getRouteById(routeId);
    }

    @GetMapping("{routeId}/trips")
    public PagedResponse<TripDto> getTripsByRoute(
            @PathVariable String routeId,
            @Valid @ModelAttribute PageRequestDto pageRequest) {

        Page<TripDto> result = tripService.getTripsByRoute(routeId, pageRequest);
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

    @GetMapping("/{routeId}/shape")
    public GeoJsonFeatureResponse<RouteShapeProperties> getShape(@PathVariable String routeId) {
        RouteDto route = routeService.getRouteById(routeId);
        ShapeDto shape = shapeService.getShapeByRouteId(routeId);
        return GeoJsonFeatureResponse.of(
                new RouteShapeProperties(route,shape.shapeId()),
                shape.geom()
        );
    }

    @PostMapping
    public RouteDto saveRoute(@RequestBody RouteDto routeDto) {
        return routeService.saveRoute(routeDto);
    }

    @PostMapping("/with-stops")
    public RouteDto createRouteWithStops(@RequestBody NewRouteRequest request) {
        return routeStopService.createRouteWithStops(request.route(), request.stopIds());
    }
}
