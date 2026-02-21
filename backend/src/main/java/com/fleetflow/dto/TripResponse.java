package com.fleetflow.dto;

import com.fleetflow.enums.TripStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TripResponse {
    private Long id;
    private Long vehicleId;
    private String vehicleName;
    private String vehicleLicensePlate;
    private Long driverId;
    private String driverName;
    private String origin;
    private String destination;
    private BigDecimal cargoWeightKg;
    private BigDecimal maxCapacityKg;
    private BigDecimal revenue;
    private BigDecimal startOdometer;
    private BigDecimal endOdometer;
    private TripStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
