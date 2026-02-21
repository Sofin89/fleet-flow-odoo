package com.fleetflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing historical location data for drivers.
 * Stores location snapshots with 90-day retention policy.
 * 
 * Requirements: 11.3, 11.4, 11.5
 */
@Entity
@Table(name = "location_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Driver ID is required")
    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    @Column(nullable = false)
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    @Column(nullable = false)
    private Double longitude;

    @DecimalMin(value = "0.0", message = "Accuracy must be >= 0")
    @Column
    private Double accuracy;

    @DecimalMin(value = "0.0", message = "Speed must be >= 0")
    @Column
    private Double speed;

    @DecimalMin(value = "0.0", message = "Heading must be >= 0")
    @DecimalMax(value = "360.0", message = "Heading must be <= 360")
    @Column
    private Double heading;

    @NotNull(message = "Recorded timestamp is required")
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
    }
}
