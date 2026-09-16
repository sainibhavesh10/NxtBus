package com.nxtbus.routing.service;

import com.nxtbus.backend.exception.IndexNotReadyException;
import com.nxtbus.backend.exception.NoJourneyFoundException;
import com.nxtbus.routing.engine.RaptorEngine;
import com.nxtbus.routing.index.RaptorIndexHolder;
import com.nxtbus.routing.index.RaptorSnapshot;
import com.nxtbus.routing.model.Journey;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RaptorServiceImpl implements RaptorService {

    private final RaptorIndexHolder raptorIndexHolder;

    public RaptorServiceImpl(RaptorIndexHolder raptorIndexHolder) {
        this.raptorIndexHolder = raptorIndexHolder;
    }

    @Override
    public Journey planJourney(String fromStopId, String toStopId, int departTimeSeconds) {
        RaptorSnapshot snapshot = raptorIndexHolder.get();
        if (snapshot == null) {
            throw new IndexNotReadyException();
        }

        RaptorEngine engine = new RaptorEngine(snapshot.index(), snapshot.excludedTrips());

        Optional<Journey> journey = engine.findEarliestArrival(fromStopId, toStopId, departTimeSeconds);
        return journey.orElseThrow(() ->
                new NoJourneyFoundException(fromStopId, toStopId, departTimeSeconds));
    }
}