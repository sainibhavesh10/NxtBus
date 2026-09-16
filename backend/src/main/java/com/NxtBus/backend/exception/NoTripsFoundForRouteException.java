package com.nxtbus.backend.exception;

import java.util.Map;

public class NoTripsFoundForRouteException extends NxtBusException {

    private final String routeId;

    public NoTripsFoundForRouteException(String routeId) {
        super("No trips found for route: " + routeId, ErrorCode.NO_TRIPS_FOUND_FOR_ROUTE);
        this.routeId = routeId;
    }

    public String getRouteId() {
        return routeId;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of("routeId", routeId);
    }
}