package com.nxtbus.backend.service;

import com.nxtbus.backend.response.UpcomingDepartureResponse;

import java.time.LocalDate;
import java.time.LocalTime;

public interface DepartureService {

    UpcomingDepartureResponse getUpcomingDepartures(
            String stopId,
            LocalDate date,
            LocalTime time,
            Integer limit
    );
}