package com.nxtbus.backend.repository;

import com.nxtbus.backend.entity.TripOutOfPath;
import com.nxtbus.backend.entity.TripOutOfPath.TripOutOfPathId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Set;

public interface TripOutOfPathRepository extends JpaRepository<TripOutOfPath, TripOutOfPathId> {

    Set<String> findTripIdByServiceDate(LocalDate serviceDate);

    boolean existsByTripIdAndServiceDate(String tripId, LocalDate serviceDate);

    void deleteByTripIdAndServiceDate(String tripId, LocalDate serviceDate);
}