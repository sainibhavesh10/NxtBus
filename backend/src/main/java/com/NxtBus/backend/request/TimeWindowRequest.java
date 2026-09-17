package com.nxtbus.backend.request;

import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public record TimeWindowRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate date,

        @DateTimeFormat(pattern = "HH:mm")
        LocalTime time,

        @Min(value = 1, message = "limit must be at least 1")
        Integer limit
) {
    private static final ZoneId DELHI_ZONE = ZoneId.of("Asia/Kolkata");

    public TimeWindowRequest {
        ZonedDateTime now = ZonedDateTime.now(DELHI_ZONE);

        if (date == null) {
            date = now.toLocalDate();
        }
        if (time == null) {
            time = now.toLocalTime();
        }
        if (limit == null) {
            limit = 3;
        }
    }
}