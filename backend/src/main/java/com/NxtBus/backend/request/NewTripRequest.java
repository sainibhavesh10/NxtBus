package com.nxtbus.backend.request;

import com.nxtbus.backend.dto.NewStopTimeDto;
import com.nxtbus.backend.dto.TripDto;

import java.util.List;

public record NewTripRequest(
        TripDto trip,
        List<NewStopTimeDto> stops
) {
}