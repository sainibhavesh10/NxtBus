package com.nxtbus.backend.service;

import com.nxtbus.backend.entity.Calendar;

import java.util.List;
import java.util.Optional;

public interface CalendarService {

    void validateCalendarExists(String serviceId);

    Calendar saveCalendar(Calendar calendar);

    List<Calendar> getCalendarsByAgencyId(String agencyId);

    Optional<Calendar> getCalendarByServiceId(String serviceId);

    void deleteCalendar(String serviceId);
}