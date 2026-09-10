package com.nxtbus.backend.service;

import com.nxtbus.routing.model.Journey;
import java.time.LocalDate;
import java.util.Optional;

public interface JourneyPlanningService {
    Optional<Journey> plan(String fromStopId, String toStopId, LocalDate date, int departTimeSeconds);
}