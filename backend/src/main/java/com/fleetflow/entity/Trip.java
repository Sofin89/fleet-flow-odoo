package com.fleetflow.entity;

import com.fleetflow.enums.TripStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trips")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    @Column
    private String originName;

    @Column
    private String destinationName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cargoWeightKg;

    @Column(precision = 12, scale = 2)
    private BigDecimal revenue;

    @Column(precision = 12, scale = 2)
    private BigDecimal startOdometer;

    @Column(precision = 12, scale = 2)
    private BigDecimal endOdometer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus status = TripStatus.DRAFT;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = TripStatus.DRAFT;
    }
}
