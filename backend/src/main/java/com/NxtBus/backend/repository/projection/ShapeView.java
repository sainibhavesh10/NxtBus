package com.nxtbus.backend.repository.projection;

import org.locationtech.jts.geom.LineString;

public interface ShapeView {
    String getShapeId();
    String getGeometryJson();
    Integer getNumPoints();
}