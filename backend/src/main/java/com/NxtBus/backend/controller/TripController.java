package com.nxtbus.backend.controller;

import com.nxtbus.backend.dto.*;
import com.nxtbus.backend.response.GeoJsonFeatureResponse;
import com.nxtbus.backend.response.TripStopSequenceResponse;
import com.nxtbus.backend.service.RouteService;
import com.nxtbus.backend.service.ShapeService;
import com.nxtbus.backend.service.StopTimeService;
import com.nxtbus.backend.service.TripService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    private final RouteService routeService;

    private final StopTimeService stopTimeService;

    private final ShapeService shapeService;

    public TripController(TripService tripService,
                          RouteService routeService,
                          StopTimeService stopTimeService,
                          ShapeService shapeService){
        this.tripService = tripService;
        this.routeService = routeService;
        this.stopTimeService = stopTimeService;
        this.shapeService = shapeService;
    }

    @GetMapping("/{tripId}")
    public TripDto getTripById(@PathVariable String tripId) {
        return tripService.getTripById(tripId);
    }

    @GetMapping("/{tripId}/stops")
    public TripStopSequenceResponse getTripStopSequence(
            @PathVariable String tripId
    ) {
        TripDto trip = getTripById(tripId);
        RouteDto route = routeService.getRouteById(trip.routeId());

        List<TimedStopSequenceDto> stops = stopTimeService.getStopSequenceForTrip(tripId);

        return new TripStopSequenceResponse(
                trip.tripId(),
                trip.routeId(),
                route.routeShortName(),
                trip.serviceId(),
                trip.tripHeadsign(),
                stops
        );
    }

    @GetMapping("/{tripId}/shape")
    public GeoJsonFeatureResponse<TripShapeProperties> getShape(@PathVariable String tripId) {
        TripDto trip = tripService.getTripById(tripId);
        ShapeDto shape = shapeService.getShapeByTripId(tripId);
        return GeoJsonFeatureResponse.of(
                new TripShapeProperties(trip,shape.shapeId()),
                shape.geom()
        );
    }
}