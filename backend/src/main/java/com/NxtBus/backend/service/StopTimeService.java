package com.nxtbus.backend.service;

import com.nxtbus.backend.dto.*;
import com.nxtbus.backend.request.TimeWindowRequest;

import java.util.List;

public interface StopTimeService {

    List<DepartureDto> getUpcomingDepartures(String stopId, TimeWindowRequest request);

    List<TimedStopSequenceDto> getStopSequenceForTrip(String tripId);

    List<StopSequenceDto> getStopSequenceForRoute(String routeId);

    TripDto createTripWithStopTimes(TripDto tripDto, List<NewStopTimeDto> stops);
}