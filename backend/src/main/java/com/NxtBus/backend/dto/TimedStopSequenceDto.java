package com.nxtbus.backend.dto;

import com.nxtbus.backend.repository.projection.TimedStopSequenceView;

import java.time.LocalTime;

public record TimedStopSequenceDto(
        Integer stopSequence,
        String stopId,
        String stopName,
        String stopCode,
        LocalTime arrivalTime,
        LocalTime departureTime,
        Double lat,
        Double lon
) {

    private static final int SECONDS_IN_DAY = 86_400;

    public static TimedStopSequenceDto from(TimedStopSequenceView stop) {
        return new TimedStopSequenceDto(
                stop.getStopSequence(),
                stop.getStopId(),
                stop.getStopName(),
                stop.getStopCode(),
                LocalTime.ofSecondOfDay(stop.getArrivalTime() % SECONDS_IN_DAY),
                LocalTime.ofSecondOfDay(stop.getDepartureTime() % SECONDS_IN_DAY),
                stop.getLat(),
                stop.getLon()
        );
    }

}