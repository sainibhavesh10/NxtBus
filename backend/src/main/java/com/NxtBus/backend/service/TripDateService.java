package com.nxtbus.backend.service;

import com.nxtbus.backend.entity.TripDate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TripDateService {

    TripDate saveTripDate(TripDate tripDate);

    List<TripDate> getTripDatesByTripId(String tripId);

    Optional<TripDate> getTripDateByTripIdAndDate(String tripId, LocalDate date);

    void deleteTripDate(TripDate.TripDateId id);
}