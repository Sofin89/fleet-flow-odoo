package com.fleetflow.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MapMarkerDTO {
    /**
     * VEHICLE = standalone vehicle (parked / no driver assigned)
     * DRIVER = standalone driver (not currently driving a vehicle)
     * COMBINED = driver is actively driving a vehicle
     */
    private String markerType; // "VEHICLE", "DRIVER", "COMBINED"

    // IDs
    private Long vehicleId;
    private Long driverId;

    // Vehicle info (null if DRIVER-only)
    private String vehicleName;
    private String vehicleModel;
    private String licensePlate;
    private String vehicleType;
    private String vehicleStatus;

    // Driver info (null if VEHICLE-only)
    private String driverName;
    private String licenseCategory;
    private String dutyStatus;
    private Integer safetyScore;
    private String licenseNumber;

    // Location
    private Double latitude;
    private Double longitude;
    private Double speed;
    private Double heading;
    private LocalDateTime lastUpdated;
}
