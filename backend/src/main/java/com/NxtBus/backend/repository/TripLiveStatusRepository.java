package com.nxtbus.backend.repository;

import com.nxtbus.backend.entity.TripLiveStatus;
import com.nxtbus.backend.entity.TripLiveStatus.TripLiveStatusId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TripLiveStatusRepository extends JpaRepository<TripLiveStatus, TripLiveStatusId> {

    // For the live map — every active trip on a given date, not RAPTOR-facing.
    List<TripLiveStatus> findByServiceDate(LocalDate serviceDate);

    Optional<TripLiveStatus> findByTripIdAndServiceDate(String tripId, LocalDate serviceDate);
}