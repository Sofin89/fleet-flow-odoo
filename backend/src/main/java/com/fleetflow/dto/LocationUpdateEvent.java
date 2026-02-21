package com.fleetflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationUpdateEvent {
    private String type;
    private Long driverId;
    private Double latitude;
    private Double longitude;
    private Double accuracy;
    private LocalDateTime timestamp;
}
