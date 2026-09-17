package com.nxtbus.backend.service.impl;

import com.nxtbus.backend.dto.*;
import com.nxtbus.backend.repository.StopTimeRepository;
import com.nxtbus.backend.request.TimeWindowRequest;
import com.nxtbus.backend.service.RouteService;
import com.nxtbus.backend.service.StopService;
import com.nxtbus.backend.service.StopTimeService;
import com.nxtbus.backend.service.TripService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StopTimeServiceImpl implements StopTimeService {

    private final StopTimeRepository stopTimeRepository;
    private final TripService tripService;
    private final StopService stopService;
    private final RouteService routeService;

    public StopTimeServiceImpl(StopTimeRepository stopTimeRepository, TripService tripService,
                               StopService stopService, RouteService routeService) {
        this.stopTimeRepository = stopTimeRepository;
        this.tripService = tripService;
        this.stopService = stopService;
        this.routeService = routeService;
    }

    @Override
    public List<DepartureDto> getUpcomingDepartures(String stopId, TimeWindowRequest request) {
        stopService.validateStopExists(stopId);
        int afterSeconds = request.time().toSecondOfDay();

        return stopTimeRepository
                .findUpcomingDepartures(stopId, request.date(), request.date().minusDays(1), afterSeconds, request.limit())
                .stream()
                .map(DepartureDto::from)
                .toList();
    }

    @Override
    public List<TimedStopSequenceDto> getStopSequenceForTrip(String tripId) {
        tripService.validateTripExists(tripId);

        return stopTimeRepository.findStopsByTripId(tripId)
                .stream()
                .map(TimedStopSequenceDto::from)
                .toList();
    }

    @Override
    public List<StopSequenceDto> getStopSequenceForRoute(String routeId) {
        routeService.validateRouteExists(routeId);
        tripService.validateRouteHasTrips(routeId);
        return stopTimeRepository.findStopSequenceByRouteId(routeId)
                .stream()
                .map(StopSequenceDto::from)
                .toList();
    }
}