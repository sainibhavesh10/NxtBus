package com.nxtbus.backend.service.impl;

import com.nxtbus.backend.repository.TripRunDateRefreshRepository;
import com.nxtbus.backend.service.TripRunDateRefreshService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class TripRunDateRefreshServiceImpl implements TripRunDateRefreshService {

    private final TripRunDateRefreshRepository repository;

    public TripRunDateRefreshServiceImpl(TripRunDateRefreshRepository repository) {
        this.repository = repository;
    }

    @Override
    public void refreshAll() {
        repository.refreshAll();
    }

    @Override
    public void onCalendarChanged(String serviceId) {
        repository.refreshForCalendar(serviceId);
    }

    @Override
    public void onCalendarDateChanged(String serviceId, LocalDate date) {
        repository.refreshForCalendarDate(serviceId, date);
    }

    @Override
    public void onTripChanged(String tripId) {
        repository.refreshForTrip(tripId);
    }

    @Override
    public void onTripDateChanged(String tripId, LocalDate date) {
        repository.refreshForTripDate(tripId, date);
    }

    @Override
    @Scheduled(cron = "0 15 0 * * *")
    public void rollWindow() {
        repository.rollWindow();
    }
}