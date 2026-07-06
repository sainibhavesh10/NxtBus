package com.nxtbus.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "gtfs_stop_times")
@IdClass(StopTime.StopTimeId.class)
@Getter @Setter @NoArgsConstructor
public class StopTime {
    @Id private String tripId;
    @Id private Integer stopSequence;

    @Column(nullable = false) private String stopId;
    @Column(nullable = false) private Integer arrivalTime;   // seconds past midnight
    @Column(nullable = false) private Integer departureTime; // seconds past midnight

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class StopTimeId implements Serializable {
        private String tripId;
        private Integer stopSequence;
    }
}
