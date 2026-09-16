package com.nxtbus.routing.service;

import com.nxtbus.routing.model.Journey;

public interface RaptorService {
    Journey planJourney(String fromStopId, String toStopId, int departTimeSeconds);
}
