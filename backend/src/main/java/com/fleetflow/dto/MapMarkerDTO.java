package com.fleetflow.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

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

    // Trip route info (only for COMBINED markers)
    private String tripOrigin;
    private String tripDestination;
    private Double originLat;
    private Double originLng;
    private Double destLat;
    private Double destLng;
    private Double totalDistanceKm;
    private Double remainingDistanceKm;
    private Double progressPercent;
    private Integer estimatedMinutesRemaining;
    
    // OSRM route data (only for COMBINED markers)
    private List<double[]> routePolyline; // [[lat, lng], ...] for road-following route
    private Boolean isRouteFallback; // true if using straight-line fallback
}
