package com.nxtbus.backend.service.impl;

import com.nxtbus.backend.dto.StopDto;
import com.nxtbus.backend.exception.NoStopNearLocationException;
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
    public Journey planJourney(String fromStopId, String toStopId, int departTimeSeconds) {
        return raptorService.planJourney(fromStopId, toStopId, departTimeSeconds);
    }

    @Override
    public Journey planJourney(double fromLat, double fromLon, double toLat, double toLon, int departTimeSeconds) {
        List<StopDto> fromCandidates = stopService.getNearestStops(new StopProximityRequest(fromLat, fromLon, 1));
        if (fromCandidates.isEmpty()) {
            throw new NoStopNearLocationException(fromLat, fromLon);
        }
        String fromStopId = fromCandidates.getFirst().stopId();

        List<StopDto> toCandidates = stopService.getNearestStops(new StopProximityRequest(toLat, toLon, 1));
        if (toCandidates.isEmpty()) {
            throw new NoStopNearLocationException(toLat, toLon);
        }
        String toStopId = toCandidates.getFirst().stopId();

        return raptorService.planJourney(fromStopId, toStopId, departTimeSeconds);
    }

}