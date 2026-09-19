package com.nxtbus.backend.exception;

import java.util.Map;

public class CalendarNotFoundException extends NxtBusException {

    private final String serviceId;

    public CalendarNotFoundException(String serviceId) {
        super("Calendar not found: " + serviceId, ErrorCode.CALENDAR_NOT_FOUND);
        this.serviceId = serviceId;
    }

    public String getServiceId() {
        return serviceId;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of("serviceId", serviceId);
    }
}