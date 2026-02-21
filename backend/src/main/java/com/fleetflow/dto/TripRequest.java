package com.fleetflow.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TripRequest {
    @NotNull(message = "Vehicle ID is required")
    private Long vehicleId;

    @NotNull(message = "Driver ID is required")
    private Long driverId;

    @NotBlank(message = "Origin is required")
    private String origin;

    @NotBlank(message = "Destination is required")
    private String destination;

    private String originName;

    private String destinationName;

    @NotNull(message = "Cargo weight is required")
    @DecimalMin(value = "0.01", message = "Cargo weight must be positive")
    private BigDecimal cargoWeightKg;

    @DecimalMin(value = "0.0", message = "Revenue cannot be negative")
    private BigDecimal revenue;
}
