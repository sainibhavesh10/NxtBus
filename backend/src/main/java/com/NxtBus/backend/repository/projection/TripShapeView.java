package com.nxtbus.backend.repository.projection;

public interface TripShapeView {
    String getTripId();
    String getRouteId();
    String getServiceId();
    String getShapeId();
    String getTripHeadsign();
    Short getDirectionId();
    String getBlockId();
    String getGeometryJson();
}