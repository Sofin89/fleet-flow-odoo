package com.fleetflow.dto;

import com.fleetflow.enums.VehicleStatus;
import com.fleetflow.enums.VehicleType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VehicleResponse {
    private Long id;
    private String name;
    private String model;
    private String licensePlate;
    private VehicleType vehicleType;
    private BigDecimal maxLoadCapacityKg;
    private BigDecimal odometerKm;
    private VehicleStatus status;
    private String region;
    private BigDecimal acquisitionCost;
    private LocalDateTime createdAt;
    private BigDecimal totalMaintenanceCost;
    private BigDecimal totalFuelCost;
}
