package com.nxtbus.backend.exception;

import java.util.Map;

public class ShapeNotFoundException extends NxtBusException {

    private final String shapeId;

    public ShapeNotFoundException(String shapeId) {
        super("Shape not found: " + shapeId, ErrorCode.SHAPE_NOT_FOUND);
        this.shapeId = shapeId;
    }

    public String getShapeId() {
        return shapeId;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of("shapeId", shapeId);
    }
}