package com.nxtbus.backend.service.impl;

import com.nxtbus.backend.entity.Calendar;
import com.nxtbus.backend.exception.CalendarNotFoundException;
import com.nxtbus.backend.repository.CalendarRepository;
import com.nxtbus.backend.service.AgencyService;
import com.nxtbus.backend.service.CalendarService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CalendarServiceImpl implements CalendarService {

    private final CalendarRepository calendarRepository;
    private final AgencyService agencyService;

    public CalendarServiceImpl(CalendarRepository calendarRepository, AgencyService agencyService) {
        this.calendarRepository = calendarRepository;
        this.agencyService = agencyService;
    }

    @Override
    public void validateCalendarExists(String serviceId) {
        if (!calendarRepository.existsById(serviceId)) {
            throw new CalendarNotFoundException(serviceId);
        }
    }

    @Override
    @Transactional
    public Calendar saveCalendar(Calendar calendar) {
        agencyService.validateAgencyExists(calendar.getAgencyId());
        return calendarRepository.save(calendar);
    }

    @Override
    public List<Calendar> getCalendarsByAgencyId(String agencyId) {
        return calendarRepository.findByAgencyId(agencyId);
    }

    @Override
    public Optional<Calendar> getCalendarByServiceId(String serviceId) {
        return calendarRepository.findById(serviceId);
    }

    @Override
    @Transactional
    public void deleteCalendar(String serviceId) {
        if (!calendarRepository.existsById(serviceId)) {
            throw new CalendarNotFoundException(serviceId);
        }
        calendarRepository.deleteById(serviceId);
    }
}