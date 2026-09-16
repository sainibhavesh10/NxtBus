package com.nxtbus.backend.exception;

import java.util.Map;

public class StopNotFoundException extends NxtBusException {

    private final String stopId;

    public StopNotFoundException(String stopId) {
        super("Stop not found: " + stopId, ErrorCode.STOP_NOT_FOUND);
        this.stopId = stopId;
    }

    public String getStopId() {
        return stopId;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of("stopId", stopId);
    }
}