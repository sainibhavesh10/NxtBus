package com.nxtbus.backend.dto;

// One entry in a caller-submitted stop list for a new trip, in the order
// the trip visits them. stop_sequence is NOT supplied here — it's derived
// by matching this list, positionally, against the route's existing
// route_stop pattern.
public record NewStopTimeDto(
        String stopId,
        Integer arrivalTime,
        Integer departureTime
) {
}