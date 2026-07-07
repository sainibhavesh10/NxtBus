package com.nxtbus.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "gtfs_agency")
@Getter @Setter @NoArgsConstructor
public class Agency {
    @Id
    @Column(name = "agency_id")
    private String agencyId;

    @Column(name = "agency_name", nullable = false)
    private String agencyName;

    private String agencyUrl;

    @Column(nullable = false)
    private String agencyTimezone;

    private String agencyLang;
    private String agencyPhone;
    private String agencyFareUrl;
}



