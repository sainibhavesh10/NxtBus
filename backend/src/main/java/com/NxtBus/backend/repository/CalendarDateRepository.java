package com.nxtbus.backend.repository;

import com.nxtbus.backend.entity.CalendarDate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CalendarDateRepository extends JpaRepository<CalendarDate, CalendarDate.CalendarDateId> {

    List<CalendarDate> findByServiceId(String serviceId);

    Optional<CalendarDate> findByServiceIdAndDate(String serviceId, LocalDate date);
}