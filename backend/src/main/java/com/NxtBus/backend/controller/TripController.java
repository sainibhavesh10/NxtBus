package com.nxtbus.backend.controller;

import com.nxtbus.backend.dto.GeoJsonFeature;
import com.nxtbus.backend.dto.RouteDto;
import com.nxtbus.backend.dto.TimedStopSequenceDto;
import com.nxtbus.backend.dto.TripDto;
import com.nxtbus.backend.response.TripStopSequenceResponse;
import com.nxtbus.backend.service.RouteService;
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

    public TripController(TripService tripService, RouteService routeService){
        this.tripService = tripService;
        this.routeService = routeService;
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

        List<TimedStopSequenceDto> stops = tripService.getTripStops(tripId);

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
    public GeoJsonFeature<TripDto> getShape(@PathVariable String tripId) {
        return tripService.getShapeForTrip(tripId);
    }
}