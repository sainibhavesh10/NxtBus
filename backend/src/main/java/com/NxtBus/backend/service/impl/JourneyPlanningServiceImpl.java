package com.nxtbus.backend.service.impl;

import com.nxtbus.backend.service.JourneyPlanningService;
import com.nxtbus.routing.engine.RaptorEngine;
import com.nxtbus.routing.index.RaptorIndexHolder;
import com.nxtbus.routing.index.RaptorSnapshot;
import com.nxtbus.routing.model.Journey;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class JourneyPlanningServiceImpl implements JourneyPlanningService {

    private final RaptorIndexHolder indexHolder;

    public JourneyPlanningServiceImpl(RaptorIndexHolder indexHolder) {
        this.indexHolder = indexHolder;
    }

    @Override
    public Optional<Journey> plan(String fromStopId, String toStopId, LocalDate date, int departTimeSeconds) {
        RaptorSnapshot snapshot = indexHolder.get();
        if (snapshot == null) return Optional.empty(); // cold start, no index built yet
        return new RaptorEngine(snapshot.index(), snapshot.excludedTrips())
                .findEarliestArrival(fromStopId, toStopId, departTimeSeconds);
    }
}