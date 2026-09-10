package com.nxtbus.backend.repository;

import com.nxtbus.backend.entity.RerouteStopTime;
import com.nxtbus.backend.entity.RerouteStopTime.RerouteStopTimeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RerouteStopTimeRepository extends JpaRepository<RerouteStopTime, RerouteStopTimeId> {

    // Bulk load for materialization, pre-ordered so grouping by tripId in Java
    // preserves stop order — no extra sort needed after fetch.
    @Query("""
        SELECT r FROM RerouteStopTime r
        WHERE r.serviceDate = :date
        ORDER BY r.tripId ASC, r.stopSequence ASC
        """)
    List<RerouteStopTime> findAllByServiceDateOrdered(@Param("date") LocalDate date);

    List<RerouteStopTime> findByTripIdAndServiceDateOrderByStopSequenceAsc(
            String tripId, LocalDate serviceDate);

    void deleteByTripIdAndServiceDate(String tripId, LocalDate serviceDate);
}