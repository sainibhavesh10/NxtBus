package com.nxtbus.backend.exception;

import java.util.Map;

public class NoStopNearLocationException extends NxtBusException {

    private final double lat;
    private final double lon;

    public NoStopNearLocationException(double lat, double lon) {
        super(String.format("No stop found near (%f, %f)", lat, lon), ErrorCode.STOP_NOT_FOUND);
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of(
                "lat", lat,
                "lon", lon
        );
    }
}