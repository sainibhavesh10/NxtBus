package com.nxtbus.backend.controller;

import com.nxtbus.backend.dto.*;
import com.nxtbus.backend.response.GeoJsonFeatureResponse;
import com.nxtbus.backend.response.PagedResponse;
import com.nxtbus.backend.response.RouteStopSequenceResponse;
import com.nxtbus.backend.service.RouteService;
import com.nxtbus.backend.service.ShapeService;
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

    private final ShapeService shapeService;

    public RoutesController(RouteService routeService,
                            TripService tripService,
                            StopTimeService stopTimeService,
                            ShapeService shapeService){
        this.routeService = routeService;
        this.tripService = tripService;
        this.stopTimeService = stopTimeService;
        this.shapeService = shapeService;
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

    @GetMapping("/{routeId}/shape")
    public GeoJsonFeatureResponse<RouteShapeProperties> getShape(@PathVariable String routeId) {
        RouteDto route = routeService.getRouteById(routeId);
        //check if the given route has a trip or not
        tripService.getRepresentativeTrip(routeId);
        ShapeDto shape = shapeService.getShapeByRouteId(routeId);
        return GeoJsonFeatureResponse.of(
                new RouteShapeProperties(route,shape.shapeId()),
                shape.geom()
        );
    }
}
