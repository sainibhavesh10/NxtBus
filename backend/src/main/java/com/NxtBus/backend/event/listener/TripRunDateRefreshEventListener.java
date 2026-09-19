package com.nxtbus.backend.event.listener;

import com.nxtbus.backend.event.CalendarChangedEvent;
import com.nxtbus.backend.event.CalendarDateChangedEvent;
import com.nxtbus.backend.event.TripChangedEvent;
import com.nxtbus.backend.event.TripDateChangedEvent;
import com.nxtbus.backend.service.TripRunDateRefreshService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TripRunDateRefreshEventListener {

    private final TripRunDateRefreshService refreshService;

    public TripRunDateRefreshEventListener(TripRunDateRefreshService refreshService) {
        this.refreshService = refreshService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCalendarChanged(CalendarChangedEvent event) {
        refreshService.onCalendarChanged(event.serviceId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCalendarDateChanged(CalendarDateChangedEvent event) {
        refreshService.onCalendarDateChanged(event.serviceId(), event.date());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTripChanged(TripChangedEvent event) {
        refreshService.onTripChanged(event.tripId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTripDateChanged(TripDateChangedEvent event) {
        refreshService.onTripDateChanged(event.tripId(), event.date());
    }
}