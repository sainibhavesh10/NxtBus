package com.nxtbus.backend.service;

import com.nxtbus.backend.request.JourneyByLocationRequest;
import com.nxtbus.backend.request.JourneyByStopsRequest;
import com.nxtbus.routing.model.Journey;

public interface JourneyService {
    Journey planJourney(JourneyByStopsRequest request);
    Journey planJourney(JourneyByLocationRequest request);
}
