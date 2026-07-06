package com.nxtbus.backend.repository.projection;

public interface UpcomingDeparture {
    String getTripId();
    Integer getDepartureTime();
    String getRouteId();
    String getRouteShortName();
    String getRouteLongName();
}
