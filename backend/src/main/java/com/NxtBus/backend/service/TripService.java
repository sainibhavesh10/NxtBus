package com.nxtbus.backend.service;

import com.nxtbus.backend.dto.RouteDto;
import com.nxtbus.backend.dto.StopDto;
import com.nxtbus.backend.dto.TimedStopSequenceDto;
import com.nxtbus.backend.dto.TripDto;
import com.nxtbus.backend.entity.Stop;
import com.nxtbus.backend.entity.Trip;
import com.nxtbus.backend.exception.StopNotFoundException;
import com.nxtbus.backend.exception.TripNotFoundException;
import com.nxtbus.backend.repository.TripRepository;
import com.nxtbus.backend.response.TripStopSequenceResponse;

import java.util.List;

public class TripService {

    private final TripRepository tripRepository;

    private final RouteService routeService;

    public TripService (TripRepository tripRepository, RouteService routeService){
        this.tripRepository = tripRepository;
        this.routeService = routeService;
    }

    public TripDto getTripById(String tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        return TripDto.from(trip);
    }

    public List<TimedStopSequenceDto> getTripStops(String tripId) {
        if (!tripRepository.existsById(tripId)) {
            throw new TripNotFoundException(tripId);
        }

        return tripRepository.findStopsByTripId(tripId)
                .stream()
                .map(TimedStopSequenceDto::from)
                .toList();
    }


}
