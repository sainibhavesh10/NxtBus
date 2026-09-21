package com.nxtbus.backend.exception;

import java.util.Map;

public class StopPatternMismatchException extends NxtBusException {

    private final String routeId;
    private final int position;
    private final String expectedStopId;
    private final String actualStopId;

    public StopPatternMismatchException(String routeId, int position, String expectedStopId, String actualStopId) {
        super("Route " + routeId + " stop pattern mismatch at position " + position
                        + ": expected stop " + expectedStopId + ", got " + actualStopId,
                ErrorCode.STOP_PATTERN_MISMATCH);
        this.routeId = routeId;
        this.position = position;
        this.expectedStopId = expectedStopId;
        this.actualStopId = actualStopId;
    }

    public StopPatternMismatchException(String routeId, int expectedLength, int actualLength) {
        super("Route " + routeId + " has " + expectedLength + " stops in its pattern, but "
                        + actualLength + " were submitted",
                ErrorCode.STOP_PATTERN_MISMATCH);
        this.routeId = routeId;
        this.position = -1;
        this.expectedStopId = null;
        this.actualStopId = null;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of(
                "routeId", routeId,
                "position", position,
                "expectedStopId", expectedStopId == null ? "" : expectedStopId,
                "actualStopId", actualStopId == null ? "" : actualStopId
        );
    }
}