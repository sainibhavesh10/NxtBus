package com.nxtbus.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "trip_run_date")
@IdClass(TripRunDate.TripRunDateId.class)
@Getter @Setter @NoArgsConstructor
public class TripRunDate {
    @Id private String tripId;
    @Id private LocalDate date;

    @Column(nullable = false)
    private Boolean running;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class TripRunDateId implements Serializable {
        private String tripId;
        private LocalDate date;
    }
}
