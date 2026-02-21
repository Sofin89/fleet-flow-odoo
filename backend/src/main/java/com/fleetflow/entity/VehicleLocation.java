package com.fleetflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_locations")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class VehicleLocation {

    @Id
    private Long vehicleId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

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
