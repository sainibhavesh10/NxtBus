package com.nxtbus.backend.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public record JourneyByLocationRequest(
        @NotNull(message = "fromLat is required")
        @DecimalMin(value = "-90.0", message = "fromLat must be >= -90")
        @DecimalMax(value = "90.0", message = "fromLat must be <= 90")
        Double fromLat,

        @NotNull(message = "fromLon is required")
        @DecimalMin(value = "-180.0", message = "fromLon must be >= -180")
        @DecimalMax(value = "180.0", message = "fromLon must be <= 180")
        Double fromLon,

        @NotNull(message = "toLat is required")
        @DecimalMin(value = "-90.0", message = "toLat must be >= -90")
        @DecimalMax(value = "90.0", message = "toLat must be <= 90")
        Double toLat,

        @NotNull(message = "toLon is required")
        @DecimalMin(value = "-180.0", message = "toLon must be >= -180")
        @DecimalMax(value = "180.0", message = "toLon must be <= 180")
        Double toLon,

        @Min(value = 0, message = "departTimeSeconds must be >= 0")
        Integer departTimeSeconds
) {
        private static final ZoneId DELHI_ZONE = ZoneId.of("Asia/Kolkata");

        public JourneyByLocationRequest {
                if (departTimeSeconds == null) {
                        LocalTime now = ZonedDateTime.now(DELHI_ZONE).toLocalTime();
                        departTimeSeconds = now.toSecondOfDay();
                }
        }
}