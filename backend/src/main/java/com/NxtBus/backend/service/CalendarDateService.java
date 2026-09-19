package com.nxtbus.backend.service;

import com.nxtbus.backend.entity.CalendarDate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CalendarDateService {

    CalendarDate saveCalendarDate(CalendarDate calendarDate);

    List<CalendarDate> getCalendarDatesByServiceId(String serviceId);

    Optional<CalendarDate> getCalendarDateByServiceIdAndDate(String serviceId, LocalDate date);

    void deleteCalendarDate(CalendarDate.CalendarDateId id);
}