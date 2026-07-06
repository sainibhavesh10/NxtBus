package com.nxtbus.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "gtfs_stops")
@Getter @Setter @NoArgsConstructor
public class Stop {
    @Id
    @Column(name = "stop_id")
    private String stopId;

    private String stopCode;

    @Column(nullable = false)
    private String stopName;

    @Column(nullable = false)
    private Double stopLat;

    @Column(nullable = false)
    private Double stopLon;

    private String zoneId;

    @Column(insertable = false, updatable = false)
    private Point geom;
}