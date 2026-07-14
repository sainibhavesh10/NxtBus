package com.nxtbus.backend.service.impl;

import com.nxtbus.backend.dto.*;
import com.nxtbus.backend.exception.NoTripsFoundForRouteException;
import com.nxtbus.backend.repository.StopTimeRepository;
import com.nxtbus.backend.response.RouteStopSequenceResponse;
import com.nxtbus.backend.service.RouteService;
import com.nxtbus.backend.service.StopTimeService;
import com.nxtbus.backend.service.TripService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class StopTimeServiceImpl implements StopTimeService {
    private static final int MAX_DEPARTURE_LIMIT = 50;

    private final StopTimeRepository stopTimeRepository;
    private final TripService tripService;

    public StopTimeServiceImpl(StopTimeRepository stopTimeRepository,
                               TripService tripService) {
        this.stopTimeRepository = stopTimeRepository;
        this.tripService = tripService;
    }

    @Override
    public List<DepartureDto> getUpcomingDepartures(String stopId,
                                                           LocalDate date,
                                                           LocalTime time,
                                                           Integer limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_DEPARTURE_LIMIT));
        int afterSeconds = time.toSecondOfDay();

        return stopTimeRepository
                .findUpcomingDepartures(stopId, date, date.minusDays(1), afterSeconds, safeLimit)
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
        TripDto representativeTrip = tripService.getRepresentativeTrip(routeId);

        return stopTimeRepository.findStopsByTripId(representativeTrip.tripId())
                .stream()
                .map(StopSequenceDto::from)
                .toList();
    }
}