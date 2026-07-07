package com.nxtbus.backend.repository;

import com.nxtbus.backend.entity.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalendarRepository extends JpaRepository<Calendar, String> {

    List<Calendar> findByAgencyId(String agencyId);
}