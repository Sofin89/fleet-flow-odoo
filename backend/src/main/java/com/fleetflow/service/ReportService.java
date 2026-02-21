package com.fleetflow.service;

import com.fleetflow.entity.*;
import com.fleetflow.enums.TripStatus;
import com.fleetflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final VehicleRepository vehicleRepository;
    private final TripRepository tripRepository;
    private final FuelLogRepository fuelLogRepository;
    private final MaintenanceLogRepository maintenanceLogRepository;

    public List<Map<String, Object>> getFuelEfficiency() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        List<Map<String, Object>> results = new ArrayList<>();

        for (Vehicle vehicle : vehicles) {
            List<Trip> completedTrips = tripRepository.findByVehicleId(vehicle.getId()).stream()
                    .filter(t -> t.getStatus() == TripStatus.COMPLETED && t.getEndOdometer() != null && t.getStartOdometer() != null)
                    .collect(Collectors.toList());

            BigDecimal totalKm = BigDecimal.ZERO;
            for (Trip trip : completedTrips) {
                totalKm = totalKm.add(trip.getEndOdometer().subtract(trip.getStartOdometer()));
            }

            BigDecimal totalLiters = fuelLogRepository.getTotalLiters(vehicle.getId());

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("vehicleId", vehicle.getId());
            row.put("vehicleName", vehicle.getName());
            row.put("licensePlate", vehicle.getLicensePlate());
            row.put("totalKm", totalKm);
            row.put("totalLiters", totalLiters);
            row.put("kmPerLiter", totalLiters.compareTo(BigDecimal.ZERO) > 0
                    ? totalKm.divide(totalLiters, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);

            results.add(row);
        }

        return results;
    }

    public List<Map<String, Object>> getVehicleROI() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        List<Map<String, Object>> results = new ArrayList<>();

        for (Vehicle vehicle : vehicles) {
            BigDecimal revenue = BigDecimal.ZERO;
            List<Trip> trips = tripRepository.findByVehicleId(vehicle.getId());
            for (Trip trip : trips) {
                if (trip.getRevenue() != null && trip.getStatus() == TripStatus.COMPLETED) {
                    revenue = revenue.add(trip.getRevenue());
                }
            }

            BigDecimal maintenanceCost = maintenanceLogRepository.getTotalMaintenanceCost(vehicle.getId());
            BigDecimal fuelCost = fuelLogRepository.getTotalFuelCost(vehicle.getId());
            BigDecimal totalCost = maintenanceCost.add(fuelCost);
            BigDecimal profit = revenue.subtract(totalCost);

            BigDecimal roi = BigDecimal.ZERO;
            if (vehicle.getAcquisitionCost() != null && vehicle.getAcquisitionCost().compareTo(BigDecimal.ZERO) > 0) {
                roi = profit.divide(vehicle.getAcquisitionCost(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("vehicleId", vehicle.getId());
            row.put("vehicleName", vehicle.getName());
            row.put("licensePlate", vehicle.getLicensePlate());
            row.put("revenue", revenue);
            row.put("maintenanceCost", maintenanceCost);
            row.put("fuelCost", fuelCost);
            row.put("totalCost", totalCost);
            row.put("profit", profit);
            row.put("acquisitionCost", vehicle.getAcquisitionCost());
            row.put("roiPercent", roi);

            results.add(row);
        }

        return results;
    }

    public List<Map<String, Object>> getOperationalCosts() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        List<Map<String, Object>> results = new ArrayList<>();

        for (Vehicle vehicle : vehicles) {
            BigDecimal maintenanceCost = maintenanceLogRepository.getTotalMaintenanceCost(vehicle.getId());
            BigDecimal fuelCost = fuelLogRepository.getTotalFuelCost(vehicle.getId());
            BigDecimal totalKm = BigDecimal.ZERO;

            List<Trip> completedTrips = tripRepository.findByVehicleId(vehicle.getId()).stream()
                    .filter(t -> t.getStatus() == TripStatus.COMPLETED && t.getEndOdometer() != null && t.getStartOdometer() != null)
                    .collect(Collectors.toList());

            for (Trip trip : completedTrips) {
                totalKm = totalKm.add(trip.getEndOdometer().subtract(trip.getStartOdometer()));
            }

            BigDecimal costPerKm = totalKm.compareTo(BigDecimal.ZERO) > 0
                    ? (maintenanceCost.add(fuelCost)).divide(totalKm, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("vehicleId", vehicle.getId());
            row.put("vehicleName", vehicle.getName());
            row.put("licensePlate", vehicle.getLicensePlate());
            row.put("maintenanceCost", maintenanceCost);
            row.put("fuelCost", fuelCost);
            row.put("totalOperationalCost", maintenanceCost.add(fuelCost));
            row.put("totalKm", totalKm);
            row.put("costPerKm", costPerKm);

            results.add(row);
        }

        return results;
    }
}
