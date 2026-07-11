package com.nxtbus.backend.repository.projection;

public interface UpcomingDepartureView {
    String getRouteId();
    String getRouteName();
    String getTripId();
    Integer getArrivalTime();
    Integer getDepartureTime();
    String getTripHeadsign();
    Integer getMinutesFromNow();
}
