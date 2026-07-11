package com.nxtbus.backend.dto;

import com.nxtbus.backend.entity.Stop;
import com.nxtbus.backend.repository.projection.StopView;

public record StopDto(
        String stopId,
        String stopCode,
        String stopName,
        Double stopLat,
        Double stopLon,
        String zoneId
) {
    public static StopDto from(Stop s) {
        return new StopDto(
                s.getStopId(), s.getStopCode(), s.getStopName(),
                s.getStopLat(), s.getStopLon(), s.getZoneId()
        );
    }

    public static StopDto from(StopView p) {
        return new StopDto(
                p.getStopId(), p.getStopCode(), p.getStopName(),
                p.getStopLat(), p.getStopLon(), p.getZoneId()
        );
    }
}