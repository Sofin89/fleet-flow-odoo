package com.fleetflow.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Response DTO for location history records.
 * Contains historical location data for a driver at a specific point in time.
 * 
 * Requirements: 11.4, 11.5
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationHistoryResponse {
    private Long id;
    private Long driverId;
    private Double latitude;
    private Double longitude;
    private Double accuracy;
    private Double speed;
    private Double heading;
    private LocalDateTime recordedAt;
}
