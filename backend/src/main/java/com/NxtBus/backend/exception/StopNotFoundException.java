package com.nxtbus.backend.exception;

public class StopNotFoundException extends RuntimeException {
    public StopNotFoundException(String stopId) {
        super("Stop not found: " + stopId);
    }
}
