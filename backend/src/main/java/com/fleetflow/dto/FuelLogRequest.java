package com.fleetflow.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class FuelLogRequest {
    @NotNull(message = "Vehicle ID is required")
    private Long vehicleId;

    private Long tripId;

    @NotNull(message = "Liters is required")
    @DecimalMin(value = "0.01", message = "Liters must be positive")
    private BigDecimal liters;

    @NotNull(message = "Cost is required")
    @DecimalMin(value = "0.0", message = "Cost cannot be negative")
    private BigDecimal cost;

    @NotNull(message = "Log date is required")
    private LocalDate logDate;
}
