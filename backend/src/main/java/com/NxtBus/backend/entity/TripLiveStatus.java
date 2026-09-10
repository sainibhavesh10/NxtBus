package com.nxtbus.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "trip_live_status")
@IdClass(TripLiveStatus.TripLiveStatusId.class)
@Getter @Setter @NoArgsConstructor
public class TripLiveStatus {
    @Id @Column(name = "trip_id") private String tripId;
    @Id @Column(name = "service_date") private LocalDate serviceDate;

    @Column(name = "driver_id")
    private String driverId;

    @Column(name = "vehicle_no")
    private String vehicleNo;

    @Column(name = "last_seq")
    private Integer lastSeq; // last confirmed departed stop_sequence — decay anchor

    @Column(name = "delay_seconds")
    private Integer delaySeconds; // null beyond last_seq + 4, never 0-as-default

    // status_1 (RUNNING/NOT_RUNNING) is intentionally NOT a column — derive it:
    @Transient
    public boolean isRunning() {
        return lastSeq != null;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class TripLiveStatusId implements Serializable {
        private String tripId;
        private LocalDate serviceDate;
    }
}