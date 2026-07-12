package com.nxtbus.backend.response;

import com.nxtbus.backend.dto.TimedStopSequenceDto;

import java.util.List;

public record TripStopSequenceResponse(
        String tripId,
        String routeId,
        String routeName,
        String serviceId,
        String tripHeadsign,
        List<TimedStopSequenceDto> stops
) {
}