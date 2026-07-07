package com.nxtbus.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "gtfs_routes")
@Getter @Setter @NoArgsConstructor
public class Route {
    @Id
    @Column(name = "route_id")
    private String routeId;

    // plain FK field, not @ManyToOne — this table is tiny (2 agencies)
    // and rarely joined in a hot path, but keeping the pattern consistent
    // means nobody accidentally adds a heavy association here later
    @Column(nullable = false)
    private String agencyId;

    private String routeShortName;
    private String routeLongName;

    @Column(nullable = false)
    private Integer routeType;

    private String routeColor;
    private String routeTextColor;
}
