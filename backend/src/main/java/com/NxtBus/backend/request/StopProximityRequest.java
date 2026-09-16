package com.nxtbus.backend.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

public record StopProximityRequest(
        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
        double lat,

        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
        double lon,

        @Min(value = 1, message = "Limit must be at least 1")
        int limit
) {
    public StopProximityRequest {
        if (limit == 0) {
            limit = 10;
        }
    }
}
