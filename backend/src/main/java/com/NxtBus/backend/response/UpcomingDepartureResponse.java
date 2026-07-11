package com.nxtbus.backend.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nxtbus.backend.dto.DepartureDto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record UpcomingDepartureResponse(
        String stopId,
        String stopName,
        LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime time,
        List<DepartureDto> departures
) {}