package com.nxtbus.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trips")
@Getter @Setter @NoArgsConstructor
public class Trip {
    @Id
    @Column(name = "trip_id")
    private String tripId;

    @Column(nullable = false) private String routeId;
    @Column(nullable = false) private String serviceId;
    private String shapeId;
    private String tripHeadsign;
    private Short directionId;
    private String blockId;
}
