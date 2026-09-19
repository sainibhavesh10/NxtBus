package com.nxtbus.backend.event;

import java.time.LocalDate;

public record CalendarDateChangedEvent(String serviceId, LocalDate date) {
}