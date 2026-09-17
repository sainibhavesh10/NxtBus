package com.nxtbus.backend.service.impl;

import com.nxtbus.backend.dto.StopDto;
import com.nxtbus.backend.exception.NoStopNearLocationException;
import com.nxtbus.backend.request.JourneyByLocationRequest;
import com.nxtbus.backend.request.JourneyByStopsRequest;
import com.nxtbus.backend.request.StopProximityRequest;
import com.nxtbus.backend.service.JourneyService;
import com.nxtbus.backend.service.StopService;
import com.nxtbus.routing.model.Journey;
import com.nxtbus.routing.service.RaptorService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JourneyServiceImpl implements JourneyService {

    private final RaptorService raptorService;
    private final StopService stopService;

    public JourneyServiceImpl(RaptorService raptorService, StopService stopService) {
        this.raptorService = raptorService;
        this.stopService = stopService;
    }

    @Override
    public Journey planJourney(JourneyByStopsRequest request) {
        stopService.validateStopExists(request.fromStopId());
        stopService.validateStopExists(request.toStopId());

        return raptorService.planJourney(request.fromStopId(), request.toStopId(), request.departTimeSeconds());
    }

    @Override
    public Journey planJourney(JourneyByLocationRequest request) {
        List<StopDto> fromCandidates = stopService.getNearestStops(
                new StopProximityRequest(request.fromLat(), request.fromLon(), 1));
        if (fromCandidates.isEmpty()) {
            throw new NoStopNearLocationException(request.fromLat(), request.fromLon());
        }
        String fromStopId = fromCandidates.getFirst().stopId();

        List<StopDto> toCandidates = stopService.getNearestStops(
                new StopProximityRequest(request.toLat(), request.toLon(), 1));
        if (toCandidates.isEmpty()) {
            throw new NoStopNearLocationException(request.toLat(), request.toLon());
        }
        String toStopId = toCandidates.getFirst().stopId();

        return raptorService.planJourney(fromStopId, toStopId, request.departTimeSeconds());
    }

}