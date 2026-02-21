package com.fleetflow.service;

import com.fleetflow.dto.DashboardKPI;
import com.fleetflow.enums.TripStatus;
import com.fleetflow.enums.VehicleStatus;
import com.fleetflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;
    private final FuelLogRepository fuelLogRepository;
    private final MaintenanceLogRepository maintenanceLogRepository;

    public DashboardKPI getKPIs() {
        long totalVehicles = vehicleRepository.count();
        long activeFleet = vehicleRepository.countByStatus(VehicleStatus.ON_TRIP);
        long maintenanceAlerts = vehicleRepository.countByStatus(VehicleStatus.IN_SHOP);
        long availableVehicles = vehicleRepository.countByStatus(VehicleStatus.AVAILABLE);
        long pendingCargo = tripRepository.countByStatus(TripStatus.DRAFT);
        long completedTrips = tripRepository.countByStatus(TripStatus.COMPLETED);
        long totalDrivers = driverRepository.count();

        double utilizationRate = totalVehicles > 0
                ? (double) (totalVehicles - availableVehicles) / totalVehicles * 100
                : 0;

        // Calculate total revenue and expenses
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        var allTrips = tripRepository.findAll();
        for (var trip : allTrips) {
            if (trip.getRevenue() != null && trip.getStatus() == TripStatus.COMPLETED) {
                totalRevenue = totalRevenue.add(trip.getRevenue());
            }
        }

        var allFuelLogs = fuelLogRepository.findAll();
        for (var log : allFuelLogs) {
            totalExpenses = totalExpenses.add(log.getCost());
        }

        var allMaintenanceLogs = maintenanceLogRepository.findAll();
        for (var log : allMaintenanceLogs) {
            totalExpenses = totalExpenses.add(log.getCost());
        }

        return DashboardKPI.builder()
                .activeFleet(activeFleet)
                .maintenanceAlerts(maintenanceAlerts)
                .utilizationRate(Math.round(utilizationRate * 100.0) / 100.0)
                .pendingCargo(pendingCargo)
                .totalVehicles(totalVehicles)
                .totalDrivers(totalDrivers)
                .completedTrips(completedTrips)
                .totalRevenue(totalRevenue)
                .totalExpenses(totalExpenses)
                .build();
    }
}
