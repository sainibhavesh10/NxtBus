package com.nxtbus.backend.exception;

import java.util.Map;

public class RouteNotFoundException extends NxtBusException {

    private final String routeId;

    public RouteNotFoundException(String routeId) {
        super("Route not found: " + routeId, ErrorCode.ROUTE_NOT_FOUND);
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