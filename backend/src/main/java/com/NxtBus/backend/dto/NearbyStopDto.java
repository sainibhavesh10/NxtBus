package com.nxtbus.backend.dto;

import com.nxtbus.backend.repository.projection.NearbyStopView;

public record NearbyStopDto(
        String stopId,
        String stopCode,
        String stopName,
        Double stopLat,
        Double stopLon,
        Double distanceMeters
) {
    public static NearbyStopDto from(NearbyStopView view) {
        return new NearbyStopDto(
                view.getStopId(),
                view.getStopCode(),
                view.getStopName(),
                view.getStopLat(),
                view.getStopLon(),
                view.getDistanceMeters()
        );
    }

}
