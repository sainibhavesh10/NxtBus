package com.nxtbus.backend.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "Route Not Found"),
    STOP_NOT_FOUND(HttpStatus.NOT_FOUND, "Stop Not Found"),
    NO_STOP_NEAR_LOCATION(HttpStatus.NOT_FOUND, "No Stop Near Location"),
    TRIP_NOT_FOUND(HttpStatus.NOT_FOUND, "Trip Not Found"),
    SHAPE_NOT_FOUND(HttpStatus.NOT_FOUND, "Shape Not Found"),
    NO_TRIPS_FOUND_FOR_ROUTE(HttpStatus.NOT_FOUND, "No Trips For Route"),
    NO_JOURNEY_FOUND(HttpStatus.NOT_FOUND, "No Journey Found"),
    INDEX_NOT_READY(HttpStatus.SERVICE_UNAVAILABLE, "Index Not Ready"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation Failed"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");

    private final HttpStatus status;
    private final String title;

    ErrorCode(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }
}