package com.nxtbus.backend.exception;

public class ShapeNotFoundException extends RuntimeException {
    public ShapeNotFoundException(String shapeId) {
        super("Shape not found: " + shapeId);
    }
}
