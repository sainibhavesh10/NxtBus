package com.nxtbus.backend.service;

import com.nxtbus.routing.model.Journey;

public interface JourneyService {
    Journey planJourney(String fromStopId, String toStopId, int departTimeSeconds);

    Journey planJourney(double fromLat, double fromLon, double toLat, double toLon, int departTimeSeconds);
}
