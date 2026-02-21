package com.fleetflow.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DriverLocationResponse {
    private Long driverId;
    private String fullName;
    private String licenseCategory;
    private String dutyStatus;
    private Integer safetyScore;
    private String licenseNumber;
    private Double latitude;
    private Double longitude;
    private Double speed;
    private Double heading;
    private LocalDateTime lastUpdated;
}
