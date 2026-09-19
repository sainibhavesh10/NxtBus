package com.nxtbus.backend.service.impl;

import com.nxtbus.backend.entity.CalendarDate;
import com.nxtbus.backend.exception.CalendarDateNotFoundException;
import com.nxtbus.backend.repository.CalendarDateRepository;
import com.nxtbus.backend.service.CalendarDateService;
import com.nxtbus.backend.service.CalendarService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CalendarDateServiceImpl implements CalendarDateService {

    private final CalendarDateRepository calendarDateRepository;
    private final CalendarService calendarService;

    public CalendarDateServiceImpl(CalendarDateRepository calendarDateRepository, CalendarService calendarService) {
        this.calendarDateRepository = calendarDateRepository;
        this.calendarService = calendarService;
    }

    @Override
    @Transactional
    public CalendarDate saveCalendarDate(CalendarDate calendarDate) {
        calendarService.validateCalendarExists(calendarDate.getServiceId());
        return calendarDateRepository.save(calendarDate);
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
    }
}