package com.nxtbus.backend.dto.realtime;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record EffectiveScheduleSnapshot(LocalDate serviceDate, List<TripSchedule> trips, Instant materializedAt) {}