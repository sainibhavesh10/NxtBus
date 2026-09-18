package com.nxtbus.backend.service;

import java.time.LocalDate;

public interface TripRunDateRefreshService {

    void refreshAll();

    void onCalendarChanged(String serviceId);

    void onCalendarDateChanged(String serviceId, LocalDate date);

    void onTripChanged(String tripId);

    void onTripDateChanged(String tripId, LocalDate date);

    void rollWindow();
}