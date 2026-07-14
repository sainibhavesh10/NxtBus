package com.nxtbus.backend.dto;

import com.nxtbus.backend.repository.projection.TimedStopSequenceView;

public record StopSequenceDto(
        Integer stopSequence,
        String stopId,
        String stopName,
        String stopCode,
        Double lat,
        Double lon
) {

    public static StopSequenceDto from(TimedStopSequenceView stop) {
        return new StopSequenceDto(
                stop.getStopSequence(),
                stop.getStopId(),
                stop.getStopName(),
                stop.getStopCode(),
                stop.getLat(),
                stop.getLon()
        );
    }
}