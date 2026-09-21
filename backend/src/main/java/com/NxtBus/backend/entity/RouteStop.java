package com.nxtbus.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "route_stop")
@IdClass(RouteStop.RouteStopId.class)
@Getter @Setter @NoArgsConstructor
public class RouteStop {

    @Id
    @Column(name = "route_id")
    private String routeId;

    @Id
    @Column(name = "stop_seq")
    private Integer stopSeq;

    @Column(name = "stop_id", nullable = false)
    private String stopId;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RouteStopId implements Serializable {
        private String routeId;
        private Integer stopSeq;
    }
}