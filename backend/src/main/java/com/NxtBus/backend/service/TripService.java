package com.nxtbus.backend.service;

import com.nxtbus.backend.dto.RouteDto;
import com.nxtbus.backend.dto.StopDto;
import com.nxtbus.backend.dto.TimedStopSequenceDto;
import com.nxtbus.backend.dto.TripDto;
import com.nxtbus.backend.entity.Stop;
import com.nxtbus.backend.entity.Trip;
import com.nxtbus.backend.exception.RouteNotFoundException;
import com.nxtbus.backend.exception.StopNotFoundException;
import com.nxtbus.backend.exception.TripNotFoundException;
import com.nxtbus.backend.repository.RouteRepository;
import com.nxtbus.backend.repository.TripRepository;
import com.nxtbus.backend.repository.projection.TripView;
import com.nxtbus.backend.response.TripStopSequenceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import java.util.List;

public class TripService {

    private static final int MAX_PAGE_SIZE = 50;

    private final TripRepository tripRepository;

    private final RouteService routeService;

    public TripService (TripRepository tripRepository, RouteService routeService){
        this.tripRepository = tripRepository;
        this.routeService = routeService;
    }

    public void validateTripExists(String tripId) {
        if (!tripRepository.existsById(tripId)) {
            throw new RouteNotFoundException(tripId);
        }
    }

    public TripDto getTripById(String tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        return TripDto.from(trip);
    }

    public List<TimedStopSequenceDto> getTripStops(String tripId) {
        validateTripExists(tripId);

        return tripRepository.findStopsByTripId(tripId)
                .stream()
                .map(TimedStopSequenceDto::from)
                .toList();
    }

    public Page<TripDto> getTripsByRoute(String routeId, int page, int size) {
        routeService.validateRouteExists(routeId);

        int cappedSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, cappedSize, Sort.by("tripId").ascending());

        return tripRepository.findByRouteId(routeId, pageable).
                map(TripDto::from);
    }
}
