package com.nxtbus.backend.repository.projection;

public interface StopView {
    String getStopId();
    String getStopCode();
    String getStopName();
    Double getStopLat();
    Double getStopLon();
    String getZoneId();
}
