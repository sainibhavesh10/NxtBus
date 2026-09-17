package com.nxtbus.backend.repository.projection;

public interface NearbyStopView {
    String getStopId();
    String getStopCode();
    String getStopName();
    Double getStopLat();
    Double getStopLon();
    String getZoneId();
    Double getDistanceMeters();
}