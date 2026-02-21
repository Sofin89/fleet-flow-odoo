package com.fleetflow.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardKPI {
    private long activeFleet;
    private long maintenanceAlerts;
    private double utilizationRate;
    private long pendingCargo;
    private long totalVehicles;
    private long totalDrivers;
    private long completedTrips;
    private BigDecimal totalRevenue;
    private BigDecimal totalExpenses;
}
