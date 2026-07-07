package com.nxtbus.backend.repository;

import com.nxtbus.backend.entity.TripRunDate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TripRunDateRepository extends JpaRepository<TripRunDate, TripRunDate.TripRunDateId> {

    List<TripRunDate> findByTripId(String tripId);

    Optional<TripRunDate> findByTripIdAndDate(String tripId, LocalDate date);

    List<TripRunDate> findByDateAndRunningTrue(LocalDate date);
}