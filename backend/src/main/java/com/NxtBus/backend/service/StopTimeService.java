package com.nxtbus.backend.service;

import com.nxtbus.backend.dto.DepartureDto;
import com.nxtbus.backend.dto.StopSequenceDto;
import com.nxtbus.backend.dto.TimedStopSequenceDto;
import com.nxtbus.backend.request.TimeWindowRequest;

import java.util.List;

public interface StopTimeService {

    List<DepartureDto> getUpcomingDepartures(String stopId, TimeWindowRequest request);

    List<TimedStopSequenceDto> getStopSequenceForTrip(String tripId);

    List<StopSequenceDto> getStopSequenceForRoute(String routeId);
}