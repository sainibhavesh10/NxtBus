package com.nxtbus.backend.dto.realtime;

import java.util.List;

// routeId is carried per-trip (not per-signature) so the builder can label
// a synthetic reroute pattern for display without routing needing to know
// GTFS route_id exists as a concept at all.
public record TripSchedule(String tripId, String routeId, List<StopTimeEntry> stopTimes) {}