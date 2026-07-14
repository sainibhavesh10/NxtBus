package com.nxtbus.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.LineString;

@Entity
@Table(name = "shapes")
@Getter
@NoArgsConstructor
public class Shape {

    @Id
    @Column(name = "shape_id")
    private String shapeId;

    @Column(
            name = "geom",
            nullable = false,
            columnDefinition = "geometry(LineString,4326)"
    )
    private LineString geom;

    @Column(name = "num_points", nullable = false)
    private Integer numPoints;
}
