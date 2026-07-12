package com.nxtbus.backend.response;

import com.nxtbus.backend.dto.StopSequenceDto;

import java.util.List;

public record RouteStopSequenceResponse (
    String routeId,
    String routeName,
    String representativeTripId,
    List<StopSequenceDto> stops
){}
