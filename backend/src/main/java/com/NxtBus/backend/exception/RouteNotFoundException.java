package com.nxtbus.backend.exception;

public class RouteNotFoundException extends RuntimeException {
    public RouteNotFoundException(String routeId) {
        super("Route not found: " + routeId);
    }
}
