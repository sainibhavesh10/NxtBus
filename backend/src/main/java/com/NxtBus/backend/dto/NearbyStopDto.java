package com.nxtbus.backend.dto;

import com.nxtbus.backend.repository.projection.NearbyStopView;

public record NearbyStopDto(
        StopDto stop,
        Double distanceMeters
) {
    public static NearbyStopDto from(NearbyStopView view) {
        StopDto stop = new StopDto(
                view.getStopId(), view.getStopCode(), view.getStopName(),
                view.getStopLat(), view.getStopLon(), view.getZoneId()
        );
        return new NearbyStopDto(stop, view.getDistanceMeters());
    }
}