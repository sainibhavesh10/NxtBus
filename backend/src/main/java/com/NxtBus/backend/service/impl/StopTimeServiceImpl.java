package com.nxtbus.backend.service.impl;

import com.nxtbus.backend.dto.*;
import com.nxtbus.backend.entity.RouteStop;
import com.nxtbus.backend.entity.StopTime;
import com.nxtbus.backend.exception.StopPatternMismatchException;
import com.nxtbus.backend.repository.StopTimeRepository;
import com.nxtbus.backend.request.TimeWindowRequest;
import com.nxtbus.backend.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class StopTimeServiceImpl implements StopTimeService {

    private final StopTimeRepository stopTimeRepository;
    private final TripService tripService;
    private final StopService stopService;
    private final RouteService routeService;
    private final RouteStopService routeStopService;

    public StopTimeServiceImpl(StopTimeRepository stopTimeRepository, TripService tripService,
                               StopService stopService, RouteService routeService, RouteStopService routeStopService) {
        this.stopTimeRepository = stopTimeRepository;
        this.tripService = tripService;
        this.stopService = stopService;
        this.routeService = routeService;
        this.routeStopService = routeStopService;
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

    @Override
    @Transactional
    public TripDto createTripWithStopTimes(TripDto tripDto, List<NewStopTimeDto> stops) {
        List<RouteStop> routeStops = routeStopService.getRouteStopsByRouteId(tripDto.routeId());

        if (routeStops.size() != stops.size()) {
            throw new StopPatternMismatchException(tripDto.routeId(), routeStops.size(), stops.size());
        }

        for (int i = 0; i < routeStops.size(); i++) {
            String expectedStopId = routeStops.get(i).getStopId();
            String actualStopId = stops.get(i).stopId();
            if (!expectedStopId.equals(actualStopId)) {
                throw new StopPatternMismatchException(tripDto.routeId(), i, expectedStopId, actualStopId);
            }
        }

        tripService.validateTripExists(tripDto.tripId());

        TripDto savedTrip = tripService.saveTrip(tripDto);

        List<StopTime> stopTimes = new ArrayList<>();
        for (int i = 0; i < routeStops.size(); i++) {
            StopTime stopTime = new StopTime();
            stopTime.setTripId(savedTrip.tripId());
            stopTime.setStopSequence(routeStops.get(i).getStopSeq());
            stopTime.setStopId(stops.get(i).stopId());
            stopTime.setArrivalTime(stops.get(i).arrivalTime());
            stopTime.setDepartureTime(stops.get(i).departureTime());
            stopTimes.add(stopTime);
        }

        stopTimeRepository.saveAll(stopTimes);

        return savedTrip;
    }
}