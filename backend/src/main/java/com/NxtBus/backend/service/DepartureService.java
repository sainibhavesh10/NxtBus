package com.nxtbus.backend.service;

import com.nxtbus.backend.dto.DepartureDto;
import com.nxtbus.backend.dto.StopDto;
import com.nxtbus.backend.response.UpcomingDepartureResponse;
import com.nxtbus.backend.repository.StopTimeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class DepartureService {
    private static final int MAX_DEPARTURE_LIMIT = 50;

    private final StopTimeRepository stopTimeRepository;
    private final StopService stopService;

    public DepartureService(StopTimeRepository stopTimeRepository,
                            StopService stopService) {
        this.stopTimeRepository = stopTimeRepository;
        this.stopService = stopService;
    }

    public UpcomingDepartureResponse getUpcomingDepartures(String stopId,
                                                           LocalDate date,
                                                           LocalTime time,
                                                           Integer limit) {

        StopDto stop = stopService.getStopById(stopId);

        int safeLimit = Math.max(1, Math.min(limit, MAX_DEPARTURE_LIMIT));
        int afterSeconds = time.toSecondOfDay();

        List<DepartureDto> departures = stopTimeRepository
                .findUpcomingDepartures(stopId, date, date.minusDays(1), afterSeconds, safeLimit)
                .stream()
                .map(DepartureDto::from)
                .toList();

        return new UpcomingDepartureResponse(
                stop.stopId(),
                stop.stopName(),
                date,
                time,
                departures
        );
    }
}