package com.nxtbus.backend.exception;

public class NoTripsFoundForRouteException extends RuntimeException {

    public NoTripsFoundForRouteException(String routeId) {
        super("No trips found for route: " + routeId);
    }
}