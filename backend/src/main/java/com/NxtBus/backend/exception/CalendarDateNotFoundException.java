package com.nxtbus.backend.exception;

import com.nxtbus.backend.entity.CalendarDate;

import java.util.Map;

public class CalendarDateNotFoundException extends NxtBusException {

    private final CalendarDate.CalendarDateId calendarDateId;

    public CalendarDateNotFoundException(CalendarDate.CalendarDateId calendarDateId) {
        super("CalendarDate not found: " + calendarDateId, ErrorCode.CALENDAR_DATE_NOT_FOUND);
        this.calendarDateId = calendarDateId;
    }

    public CalendarDate.CalendarDateId getCalendarDateId() {
        return calendarDateId;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of("calendarDateId", calendarDateId);
    }
}