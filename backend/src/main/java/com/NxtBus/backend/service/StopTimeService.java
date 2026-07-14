package com.nxtbus.backend.service;

import com.nxtbus.backend.dto.DepartureDto;
import com.nxtbus.backend.dto.StopSequenceDto;
import com.nxtbus.backend.dto.TimedStopSequenceDto;
import com.nxtbus.backend.response.RouteStopSequenceResponse;
import com.nxtbus.backend.response.UpcomingDepartureResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface StopTimeService {

    List<DepartureDto> getUpcomingDepartures(
            String stopId,
            LocalDate date,
            LocalTime time,
            Integer limit
    );

    List<TimedStopSequenceDto> getStopSequenceForTrip(String tripId);

    List<StopSequenceDto> getStopSequenceForRoute(String routeId);
}