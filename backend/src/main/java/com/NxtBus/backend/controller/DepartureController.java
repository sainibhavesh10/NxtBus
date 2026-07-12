package com.nxtbus.backend.controller;

import com.nxtbus.backend.response.UpcomingDepartureResponse;
import com.nxtbus.backend.service.DepartureService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@RestController
@RequestMapping("/api/stops")
public class DepartureController {

    private static final ZoneId DELHI_ZONE = ZoneId.of("Asia/Kolkata");

    private final DepartureService departureService;

    public DepartureController(DepartureService departureService) {
        this.departureService = departureService;
    }

    @GetMapping("/{stopId}/departures")
    public UpcomingDepartureResponse getUpcomingDepartures(
            @PathVariable String stopId,
            @RequestParam(defaultValue = "3") int limit,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "HH:mm") LocalTime time) {

        ZonedDateTime now = ZonedDateTime.now(DELHI_ZONE);

        LocalDate effectiveDate = (date != null) ? date : now.toLocalDate();
        LocalTime effectiveTime = (time != null) ? time : now.toLocalTime();

        return departureService
                .getUpcomingDepartures(stopId, effectiveDate, effectiveTime, limit);
    }
}