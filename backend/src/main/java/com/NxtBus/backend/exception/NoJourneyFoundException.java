package com.nxtbus.backend.exception;

import java.util.Map;

public class NoJourneyFoundException extends NxtBusException {

    private final String fromStopId;
    private final String toStopId;
    private final int departTimeSeconds;

    public NoJourneyFoundException(String fromStopId, String toStopId, int departTimeSeconds) {
        super(String.format("No journey found from stop %s to %s departing at %d seconds",
                fromStopId, toStopId, departTimeSeconds), ErrorCode.NO_JOURNEY_FOUND);
        this.fromStopId = fromStopId;
        this.toStopId = toStopId;
        this.departTimeSeconds = departTimeSeconds;
    }

    public String getFromStopId() {
        return fromStopId;
    }

    public String getToStopId() {
        return toStopId;
    }

    public int getDepartTimeSeconds() {
        return departTimeSeconds;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of(
                "fromStopId", fromStopId,
                "toStopId", toStopId,
                "departTimeSeconds", departTimeSeconds
        );
    }
}