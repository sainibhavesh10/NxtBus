package com.nxtbus.backend.event;

import java.time.LocalDate;

public record TripDateChangedEvent(String tripId, LocalDate date) {
}