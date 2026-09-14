package com.nxtbus.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "footpaths")
@IdClass(Footpath.FootpathId.class)
@Getter @Setter @NoArgsConstructor
public class Footpath {
    @Id @Column(name = "from_stop_id") private String fromStopId;
    @Id @Column(name = "to_stop_id") private String toStopId;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class FootpathId implements Serializable {
        private String fromStopId;
        private String toStopId;
    }
}