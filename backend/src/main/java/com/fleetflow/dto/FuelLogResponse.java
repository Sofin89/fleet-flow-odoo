package com.fleetflow.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FuelLogResponse {
    private Long id;
    private Long vehicleId;
    private String vehicleName;
    private String vehicleLicensePlate;
    private Long tripId;
    private BigDecimal liters;
    private BigDecimal cost;
    private LocalDate logDate;
    private LocalDateTime createdAt;
}
