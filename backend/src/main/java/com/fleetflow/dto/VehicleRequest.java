package com.fleetflow.dto;

import com.fleetflow.enums.VehicleStatus;
import com.fleetflow.enums.VehicleType;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class VehicleRequest {
    @NotBlank(message = "Vehicle name is required")
    private String name;

    @NotBlank(message = "Model is required")
    private String model;

    @NotBlank(message = "License plate is required")
    private String licensePlate;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @NotNull(message = "Max load capacity is required")
    @DecimalMin(value = "0.01", message = "Max load capacity must be positive")
    private BigDecimal maxLoadCapacityKg;

    @DecimalMin(value = "0.0", message = "Odometer cannot be negative")
    private BigDecimal odometerKm;

    private VehicleStatus status;
    private String region;

    @DecimalMin(value = "0.0", message = "Acquisition cost cannot be negative")
    private BigDecimal acquisitionCost;
}
