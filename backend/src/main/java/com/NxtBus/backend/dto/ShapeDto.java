package com.nxtbus.backend.dto;

import com.nxtbus.backend.repository.projection.ShapeView;

public record ShapeDto (
        String shapeId,
        String geom,
        Integer numPoints
)
{
    public static ShapeDto from(ShapeView v) {
        return new ShapeDto(v.getShapeId(), v.getGeometryJson(), v.getNumPoints());
    }
};
