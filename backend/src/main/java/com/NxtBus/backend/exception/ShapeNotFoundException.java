package com.nxtbus.backend.exception;

import java.util.Map;

public class ShapeNotFoundException extends NxtBusException {

    private final String lookupType;  // "trip", "route", or "shapeId"
    private final String lookupValue;

    public ShapeNotFoundException(String lookupType, String lookupValue) {
        super("No shape found for " + lookupType + ": " + lookupValue, ErrorCode.SHAPE_NOT_FOUND);
        this.lookupType = lookupType;
        this.lookupValue = lookupValue;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of("lookupType", lookupType, "lookupValue", lookupValue);
    }
}