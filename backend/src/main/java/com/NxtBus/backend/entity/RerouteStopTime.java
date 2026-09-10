package com.nxtbus.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "rerouted_trips")
@IdClass(RerouteStopTime.RerouteStopTimeId.class)
@Getter @Setter @NoArgsConstructor
public class RerouteStopTime {
    @Id @Column(name = "trip_id") private String tripId;
    @Id @Column(name = "service_date") private LocalDate serviceDate;
    @Id @Column(name = "stop_sequence") private Integer stopSequence;

    @Column(name = "stop_id", nullable = false)
    private String stopId;

    @Column(name = "arr_time", nullable = false)
    private Integer arrTime; // seconds past midnight, same convention as base stop_times

    @Column(name = "dep_time", nullable = false)
    private Integer depTime;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RerouteStopTimeId implements Serializable {
        private String tripId;
        private LocalDate serviceDate;
        private Integer stopSequence;
    }
}