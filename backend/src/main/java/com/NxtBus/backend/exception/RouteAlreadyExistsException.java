package com.nxtbus.backend.exception;

import java.util.Map;

public class RouteAlreadyExistsException extends NxtBusException {

    private final String routeId;

    public RouteAlreadyExistsException(String routeId) {
        super("Route already exists: " + routeId, ErrorCode.ROUTE_ALREADY_EXISTS);
        this.routeId = routeId;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of("routeId", routeId);
    }
}