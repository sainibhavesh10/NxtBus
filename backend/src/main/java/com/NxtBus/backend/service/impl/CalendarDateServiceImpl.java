package com.nxtbus.backend.service.impl;

import com.nxtbus.backend.entity.CalendarDate;
import com.nxtbus.backend.event.CalendarDateChangedEvent;
import com.nxtbus.backend.exception.CalendarDateNotFoundException;
import com.nxtbus.backend.repository.CalendarDateRepository;
import com.nxtbus.backend.service.CalendarDateService;
import com.nxtbus.backend.service.CalendarService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CalendarDateServiceImpl implements CalendarDateService {

    private final CalendarDateRepository calendarDateRepository;
    private final CalendarService calendarService;
    private final ApplicationEventPublisher events;

    public CalendarDateServiceImpl(CalendarDateRepository calendarDateRepository, CalendarService calendarService, ApplicationEventPublisher events) {
        this.calendarDateRepository = calendarDateRepository;
        this.calendarService = calendarService;
        this.events = events;
    }

    @Override
    @Transactional
    public CalendarDate saveCalendarDate(CalendarDate calendarDate) {
        calendarService.validateCalendarExists(calendarDate.getServiceId());
        CalendarDate saved = calendarDateRepository.save(calendarDate);
        events.publishEvent(new CalendarDateChangedEvent(saved.getServiceId(), saved.getDate()));
        return saved;
    }

    @Override
    public List<CalendarDate> getCalendarDatesByServiceId(String serviceId) {
        return calendarDateRepository.findByServiceId(serviceId);
    }

    @Override
    public Optional<CalendarDate> getCalendarDateByServiceIdAndDate(String serviceId, LocalDate date) {
        return calendarDateRepository.findByServiceIdAndDate(serviceId, date);
    }

    @Override
    @Transactional
    public void deleteCalendarDate(CalendarDate.CalendarDateId id) {
        if (!calendarDateRepository.existsById(id)) {
            throw new CalendarDateNotFoundException(id);
        }
        calendarDateRepository.deleteById(id);
        events.publishEvent(new CalendarDateChangedEvent(id.getServiceId(), id.getDate()));
    }
}