package com.nxtbus.backend.exception;

import java.util.Map;

public class TripNotFoundException extends NxtBusException {

    private final String tripId;

    public TripNotFoundException(String tripId) {
        super("Trip not found: " + tripId, ErrorCode.TRIP_NOT_FOUND);
        this.tripId = tripId;
    }

    public String getTripId() {
        return tripId;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of("tripId", tripId);
    }
}