package com.nxtbus.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "trip_dates")
@IdClass(TripDate.TripDateId.class)
@Getter @Setter @NoArgsConstructor
public class TripDate {
    @Id private String tripId;
    @Id private LocalDate date;

    @Column(nullable = false)
    private Short exceptionType;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class TripDateId implements Serializable {
        private String tripId;
        private LocalDate date;
    }
}
