package com.nxtbus.backend.service.impl;

import com.nxtbus.backend.service.JourneyService;
import com.nxtbus.backend.service.StopService;
import com.nxtbus.routing.model.Journey;
import com.nxtbus.routing.service.RaptorService;
import org.springframework.stereotype.Service;

@Service
public class JourneyServiceImpl implements JourneyService {

    private final RaptorService raptorService;
    private final StopService stopService;

    public JourneyServiceImpl(RaptorService raptorService, StopService stopService) {
        this.raptorService = raptorService;
        this.stopService = stopService;
    }

    @Override
    public Journey planJourney(String fromStopId, String toStopId, int departTimeSeconds) {
        return raptorService.planJourney(fromStopId, toStopId, departTimeSeconds);
    }

    @Override
    public Journey planJourney(double fromLat, double fromLon, double toLat, double toLon, int departTimeSeconds) {
        String fromStopId = stopService.getNearestStop(fromLat, fromLon).stopId();
        String toStopId = stopService.getNearestStop(toLat, toLon).stopId();

        return raptorService.planJourney(fromStopId, toStopId, departTimeSeconds);
    }
}