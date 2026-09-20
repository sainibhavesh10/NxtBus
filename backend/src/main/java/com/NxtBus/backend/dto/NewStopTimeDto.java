package com.nxtbus.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record NewStopTimeDto(
        String stopId,

        @NotNull(message = "arrivalTime must not be null")
        @Min(value = 0, message = "arrivalTime must be >= 0 (seconds past midnight)")
        Integer arrivalTime,

        @NotNull(message = "departureTime must not be null")
        @Min(value = 0, message = "departureTime must be >= 0 (seconds past midnight)")
        Integer departureTime
) {
}