package com.nxtbus.backend.service.impl;

import com.nxtbus.backend.dto.*;
import com.nxtbus.backend.entity.Trip;
import com.nxtbus.backend.exception.*;
import com.nxtbus.backend.repository.TripRepository;
import com.nxtbus.backend.request.PageRequestDto;
import com.nxtbus.backend.service.RouteService;
import com.nxtbus.backend.service.TripService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class TripServiceImpl implements TripService {

    private static final int MAX_PAGE_SIZE = 50;

    private final TripRepository tripRepository;

    private final RouteService routeService;


    public TripServiceImpl(TripRepository tripRepository, RouteService routeService){
        this.tripRepository = tripRepository;
        this.routeService = routeService;
    }

    @Override
    public void validateTripExists(String tripId) {
        if (!tripRepository.existsById(tripId)) {
            throw new TripNotFoundException(tripId);
        }
    }

    @Override
    public TripDto getTripById(String tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        return TripDto.from(trip);
    }

    @Override
    public TripDto getRepresentativeTrip(String routeId) {
        Page<TripDto> trips = getTripsByRoute(routeId, new PageRequestDto(0, 1)); // page 0, size 1 -> first tripId alphabetically
        if (trips.isEmpty()) {
            throw new NoTripsFoundForRouteException(routeId);
        }
        return trips.getContent().getFirst();
    }

    @Override
    public Page<TripDto> getTripsByRoute(String routeId, PageRequestDto pageRequest) {
        int cappedSize = Math.min(pageRequest.size(), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageRequest.page(), cappedSize, Sort.by("tripId").ascending());

        return tripRepository.findByRouteId(routeId, pageable).map(TripDto::from);
    }

}
