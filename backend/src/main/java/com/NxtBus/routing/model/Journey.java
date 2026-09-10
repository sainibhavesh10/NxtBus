package com.nxtbus.routing.model;

import java.util.List;

public record Journey(int departTimeSeconds, int arrivalTimeSeconds, List<JourneyLeg> legs) {}