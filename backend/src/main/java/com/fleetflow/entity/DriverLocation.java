package com.fleetflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_locations")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DriverLocation {

    @Id
    private Long driverId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Double speed;

    @Column
    private Double heading;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;
}
