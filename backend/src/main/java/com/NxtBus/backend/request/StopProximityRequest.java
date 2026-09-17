package com.nxtbus.backend.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StopProximityRequest(
        @NotNull(message = "lat is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
        Double lat,

        @NotNull(message = "lon is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
        Double lon,

        @Min(value = 1, message = "Limit must be at least 1")
        Integer limit
) {
    public StopProximityRequest {
        if (limit == null) {
            limit = 10;
        }
    }
}