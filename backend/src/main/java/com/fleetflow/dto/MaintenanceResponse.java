package com.fleetflow.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MaintenanceResponse {
    private Long id;
    private Long vehicleId;
    private String vehicleName;
    private String vehicleLicensePlate;
    private String serviceType;
    private String description;
    private BigDecimal cost;
    private LocalDate serviceDate;
    private Boolean completed;
    private LocalDateTime createdAt;
}
