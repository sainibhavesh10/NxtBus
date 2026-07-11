package com.nxtbus.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "calendar_dates")
@IdClass(CalendarDate.CalendarDateId.class)
@Getter @Setter @NoArgsConstructor
public class CalendarDate {
    @Id @Column(name = "service_id") private String serviceId;
    @Id private LocalDate date;

    @Column(nullable = false)
    private Short exceptionType; // 1 = added, 2 = removed

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CalendarDateId implements Serializable {
        private String serviceId;
        private LocalDate date;
    }
}

