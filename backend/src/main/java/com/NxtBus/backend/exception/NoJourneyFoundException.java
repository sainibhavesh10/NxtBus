package com.nxtbus.backend.exception;

public class NoJourneyFoundException extends RuntimeException {

    public NoJourneyFoundException(String fromStopId, String toStopId, int departTimeSeconds) {
        super(String.format("No journey found from stop %s to %s departing at %d seconds",
                fromStopId, toStopId, departTimeSeconds));
    }
}