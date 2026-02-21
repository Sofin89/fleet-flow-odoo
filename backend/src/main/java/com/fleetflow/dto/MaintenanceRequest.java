package com.fleetflow.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MaintenanceRequest {
    @NotNull(message = "Vehicle ID is required")
    private Long vehicleId;

    @NotBlank(message = "Service type is required")
    private String serviceType;

    private String description;

    @NotNull(message = "Cost is required")
    @DecimalMin(value = "0.0", message = "Cost cannot be negative")
    private BigDecimal cost;

    @NotNull(message = "Service date is required")
    private LocalDate serviceDate;
}
