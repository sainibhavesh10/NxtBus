package com.nxtbus.backend.service.impl;

import com.nxtbus.backend.dto.*;
import com.nxtbus.backend.entity.Trip;
import com.nxtbus.backend.event.TripChangedEvent;
import com.nxtbus.backend.exception.*;
import com.nxtbus.backend.repository.TripRepository;
import com.nxtbus.backend.request.PageRequestDto;
import com.nxtbus.backend.service.RouteService;
import com.nxtbus.backend.service.TripService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripServiceImpl implements TripService {

    private static final int MAX_PAGE_SIZE = 50;

    private final TripRepository tripRepository;
    private final RouteService routeService;
    private final ApplicationEventPublisher events;

    public TripServiceImpl(TripRepository tripRepository, RouteService routeService, ApplicationEventPublisher events){
        this.tripRepository = tripRepository;
        this.routeService = routeService;
        this.events = events;
    }

    @Override
    public void validateTripExists(String tripId) {
        if (!tripRepository.existsById(tripId)) {
            throw new TripNotFoundException(tripId);
        }
    }

    @Override
    public void validateRouteHasTrips(String routeId) {
        if (!tripRepository.existsByRouteId(routeId)) {
            throw new NoTripsFoundForRouteException(routeId);
        }
    }

    @Override
    public TripDto getTripById(String tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        return TripDto.from(trip);
    }

    @Override
    public Page<TripDto> getTripsByRoute(String routeId, PageRequestDto pageRequest) {
        int cappedSize = Math.min(pageRequest.size(), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageRequest.page(), cappedSize, Sort.by("tripId").ascending());

        return tripRepository.findByRouteId(routeId, pageable).map(TripDto::from);
    }

    @Override
    @Transactional
    public TripDto saveTrip(TripDto tripDto) {
        routeService.validateRouteExists(tripDto.routeId());
        Trip trip = tripDto.toEntity();

        Trip savedTrip = tripRepository.save(trip);
        events.publishEvent(new TripChangedEvent(savedTrip.getTripId()));
        return TripDto.from(savedTrip);
    }

    @Override
    @Transactional
    public void deleteTrip(String tripId) {
        validateTripExists(tripId);
        tripRepository.deleteById(tripId);
        events.publishEvent(new TripChangedEvent(tripId));
    }
}