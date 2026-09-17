package com.nxtbus.backend.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public record JourneyByStopsRequest(
        @NotBlank(message = "fromStopId must not be blank")
        String fromStopId,

        @NotBlank(message = "toStopId must not be blank")
        String toStopId,

        @Min(value = 0, message = "departTimeSeconds must be >= 0")
        Integer departTimeSeconds
) {
        private static final ZoneId DELHI_ZONE = ZoneId.of("Asia/Kolkata");

        public JourneyByStopsRequest {
                if (departTimeSeconds == null) {
                        LocalTime now = ZonedDateTime.now(DELHI_ZONE).toLocalTime();
                        departTimeSeconds = now.toSecondOfDay();
                }
        }
}