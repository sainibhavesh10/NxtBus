package com.nxtbus.backend.controller;

import com.nxtbus.backend.dto.TripDto;
import com.nxtbus.backend.response.TripStopSequenceResponse;
import com.nxtbus.backend.service.TripService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService){
        this.tripService = tripService;
    }

    @GetMapping("/{tripId}")
    public TripDto getTripById(@PathVariable String tripId) {
        return tripService.getTripById(tripId);
    }

    @GetMapping("/{tripId}/stops")
    public TripStopSequenceResponse getTripStopSequence(
            @PathVariable String tripId
    ) {
        return tripService.getTripStopSequence(tripId);
    }
}