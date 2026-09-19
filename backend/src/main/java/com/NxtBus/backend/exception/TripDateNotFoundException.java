package com.nxtbus.backend.exception;

import com.nxtbus.backend.entity.TripDate;

import java.util.Map;

public class TripDateNotFoundException extends NxtBusException {

    private final TripDate.TripDateId tripDateId;

    public TripDateNotFoundException(TripDate.TripDateId tripDateId) {
        super("TripDate not found: " + tripDateId, ErrorCode.TRIP_DATE_NOT_FOUND);
        this.tripDateId = tripDateId;
    }

    public TripDate.TripDateId getTripDateId() {
        return tripDateId;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of("tripDateId", tripDateId);
    }
}