package com.nxtbus.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nxtbus.backend.repository.projection.UpcomingDepartureView;

import java.time.LocalTime;

public record DepartureDto(
        String routeId,
        String routeName,
        String tripId,
        @JsonFormat(pattern = "HH:mm") LocalTime arrivalTime,
        @JsonFormat(pattern = "HH:mm") LocalTime departureTime,
        @JsonProperty("headsign") String tripHeadsign,
        boolean nextDay,
        int minutesFromNow
) {
    private static final int SECONDS_IN_DAY = 86_400;

    public static DepartureDto from(UpcomingDepartureView v) {
        return new DepartureDto(
                v.getRouteId(), v.getRouteName(), v.getTripId(),
                LocalTime.ofSecondOfDay(v.getArrivalTime() % SECONDS_IN_DAY),
                LocalTime.ofSecondOfDay(v.getDepartureTime() % SECONDS_IN_DAY),
                v.getTripHeadsign(),
                v.getDepartureTime() >= SECONDS_IN_DAY,
                v.getMinutesFromNow()
        );
    }
}