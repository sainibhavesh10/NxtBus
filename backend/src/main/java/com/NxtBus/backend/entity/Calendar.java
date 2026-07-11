package com.nxtbus.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "calendar")
@Getter
@Setter @NoArgsConstructor
public class Calendar {
    @Id
    @Column(name = "service_id")
    private String serviceId;

    @Column(nullable = false) private String agencyId;
    @Column(nullable = false) private LocalDate startDate;
    @Column(nullable = false) private LocalDate endDate;
    @Column(nullable = false) private Boolean monday;
    @Column(nullable = false) private Boolean tuesday;
    @Column(nullable = false) private Boolean wednesday;
    @Column(nullable = false) private Boolean thursday;
    @Column(nullable = false) private Boolean friday;
    @Column(nullable = false) private Boolean saturday;
    @Column(nullable = false) private Boolean sunday;
}

