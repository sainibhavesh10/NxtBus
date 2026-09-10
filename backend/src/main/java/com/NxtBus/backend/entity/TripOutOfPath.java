package com.nxtbus.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "trip_out_of_path")
@IdClass(TripOutOfPath.TripOutOfPathId.class)
@Getter @Setter @NoArgsConstructor
public class TripOutOfPath {
    @Id @Column(name = "trip_id") private String tripId;
    @Id @Column(name = "service_date") private LocalDate serviceDate;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class TripOutOfPathId implements Serializable {
        private String tripId;
        private LocalDate serviceDate;
    }
}