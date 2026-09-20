package com.nxtbus.backend.exception;

import java.util.Map;

public class TripAlreadyExistsException extends NxtBusException {

    private final String tripId;

    public TripAlreadyExistsException(String tripId) {
        super("Trip already exists: " + tripId, ErrorCode.TRIP_ALREADY_EXISTS);
        this.tripId = tripId;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of("tripId", tripId);
    }
}