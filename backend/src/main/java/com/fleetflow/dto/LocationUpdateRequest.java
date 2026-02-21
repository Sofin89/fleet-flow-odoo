package com.fleetflow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO for location updates from drivers.
 * Contains GPS coordinates and optional metadata like accuracy, speed, and heading.
 * 
 * Requirements: 1.2, 3.1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationUpdateRequest {
    
    @NotNull(message = "Latitude is required")
    @Min(value = -90, message = "Latitude must be >= -90")
    @Max(value = 90, message = "Latitude must be <= 90")
    private Double latitude;
    
    @NotNull(message = "Longitude is required")
    @Min(value = -180, message = "Longitude must be >= -180")
    @Max(value = 180, message = "Longitude must be <= 180")
    private Double longitude;
    
    @Min(value = 0, message = "Accuracy must be >= 0")
    private Double accuracy;
    
    @Min(value = 0, message = "Speed must be >= 0")
    private Double speed;
    
    @Min(value = 0, message = "Heading must be >= 0")
    @Max(value = 360, message = "Heading must be <= 360")
    private Double heading;
}
