package com.fleetflow.service;

import com.fleetflow.entity.FuelLog;
import com.fleetflow.entity.Trip;
import com.fleetflow.entity.Vehicle;
import com.fleetflow.enums.TripStatus;
import com.fleetflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Financial intelligence for admin: cost per trip, fuel efficiency per vehicle,
 * profit per route, fraud risk (fuel anomalies).
 */
@Service
@RequiredArgsConstructor
public class FinancialIntelligenceService {

    private final VehicleRepository vehicleRepository;
    private final TripRepository tripRepository;
    private final FuelLogRepository fuelLogRepository;
    private final MaintenanceLogRepository maintenanceLogRepository;

    public List<Map<String, Object>> getCostPerTrip() {
        List<Trip> completed = tripRepository.findByStatus(TripStatus.COMPLETED);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Trip t : completed) {
            BigDecimal fuelCost = fuelLogRepository.getTotalFuelCost(t.getVehicle().getId());
            long tripCount = tripRepository.findByVehicleId(t.getVehicle().getId()).stream()
                    .filter(x -> x.getStatus() == TripStatus.COMPLETED).count();
            BigDecimal maintenanceCost = maintenanceLogRepository.getTotalMaintenanceCost(t.getVehicle().getId());
            BigDecimal totalCost = fuelCost.add(maintenanceCost);
            BigDecimal costPerTrip = tripCount > 0
                    ? totalCost.divide(BigDecimal.valueOf(tripCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tripId", t.getId());
            row.put("origin", t.getOrigin());
            row.put("destination", t.getDestination());
            row.put("vehicleName", t.getVehicle().getName());
            row.put("totalCost", totalCost);
            row.put("costPerTrip", costPerTrip);
            row.put("revenue", t.getRevenue());
            result.add(row);
        }
        // Dedupe by trip (we were iterating trips so one row per trip is fine)
        return result;
    }

    public List<Map<String, Object>> getFuelEfficiencyPerVehicle() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Vehicle v : vehicles) {
            BigDecimal totalLiters = fuelLogRepository.getTotalLiters(v.getId());
            List<Trip> completed = tripRepository.findByVehicleId(v.getId()).stream()
                    .filter(t -> t.getStatus() == TripStatus.COMPLETED && t.getEndOdometer() != null && t.getStartOdometer() != null)
                    .collect(Collectors.toList());
            BigDecimal totalKm = completed.stream()
                    .map(t -> t.getEndOdometer().subtract(t.getStartOdometer()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal kmPerLiter = totalLiters.compareTo(BigDecimal.ZERO) > 0
                    ? totalKm.divide(totalLiters, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("vehicleId", v.getId());
            row.put("vehicleName", v.getName());
            row.put("licensePlate", v.getLicensePlate());
            row.put("totalKm", totalKm);
            row.put("totalLiters", totalLiters);
            row.put("kmPerLiter", kmPerLiter);
            result.add(row);
        }
        return result;
    }

    public List<Map<String, Object>> getProfitPerRoute() {
        List<Trip> completed = tripRepository.findByStatus(TripStatus.COMPLETED);
        Map<String, Map<String, Object>> byRoute = new LinkedHashMap<>();

        for (Trip t : completed) {
            String key = t.getOrigin() + " → " + t.getDestination();
            BigDecimal revenue = t.getRevenue() != null ? t.getRevenue() : BigDecimal.ZERO;
            BigDecimal fuelCost = fuelLogRepository.getTotalFuelCost(t.getVehicle().getId());
            long vehicleTrips = tripRepository.findByVehicleId(t.getVehicle().getId()).stream()
                    .filter(x -> x.getStatus() == TripStatus.COMPLETED).count();
            BigDecimal maintenanceCost = maintenanceLogRepository.getTotalMaintenanceCost(t.getVehicle().getId());
            BigDecimal totalVehicleCost = fuelCost.add(maintenanceCost);
            BigDecimal costThisTrip = vehicleTrips > 0
                    ? totalVehicleCost.divide(BigDecimal.valueOf(vehicleTrips), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal profit = revenue.subtract(costThisTrip);

            byRoute.putIfAbsent(key, new LinkedHashMap<>());
            Map<String, Object> row = byRoute.get(key);
            row.put("route", key);
            row.put("tripCount", (Integer) row.getOrDefault("tripCount", 0) + 1);
            row.put("totalRevenue", ((BigDecimal) row.getOrDefault("totalRevenue", BigDecimal.ZERO)).add(revenue));
            row.put("totalCost", ((BigDecimal) row.getOrDefault("totalCost", BigDecimal.ZERO)).add(costThisTrip));
            row.put("totalProfit", ((BigDecimal) row.getOrDefault("totalProfit", BigDecimal.ZERO)).add(profit));
        }

        return new ArrayList<>(byRoute.values());
    }

    /**
     * Fraud risk score based on fuel anomalies: high cost/liter or outlier liters per vehicle.
     */
    public List<Map<String, Object>> getFraudRiskScore() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        List<FuelLog> allLogs = fuelLogRepository.findAll();
        if (allLogs.isEmpty()) return Collections.emptyList();

        double globalAvgCostPerLiter = allLogs.stream()
                .map(f -> f.getCost().doubleValue() / Math.max(f.getLiters().doubleValue(), 0.001))
                .average().orElse(0);
        double globalStdCost = allLogs.stream()
                .map(f -> f.getCost().doubleValue() / Math.max(f.getLiters().doubleValue(), 0.001))
                .mapToDouble(x -> Math.pow(x - globalAvgCostPerLiter, 2)).average().orElse(0);
        globalStdCost = Math.sqrt(globalStdCost);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Vehicle v : vehicles) {
            List<FuelLog> logs = fuelLogRepository.findByVehicleId(v.getId());
            if (logs.isEmpty()) continue;

            double avgCostPerLiter = logs.stream()
                    .map(f -> f.getCost().doubleValue() / Math.max(f.getLiters().doubleValue(), 0.001))
                    .average().orElse(0);
            double zScore = globalStdCost > 0 ? (avgCostPerLiter - globalAvgCostPerLiter) / globalStdCost : 0;
            double fraudScore = Math.min(100, Math.max(0, 50 + zScore * 20));

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("vehicleId", v.getId());
            row.put("vehicleName", v.getName());
            row.put("licensePlate", v.getLicensePlate());
            row.put("avgCostPerLiter", BigDecimal.valueOf(avgCostPerLiter).setScale(2, RoundingMode.HALF_UP));
            row.put("fraudRiskScore", Math.round(fraudScore * 100.0) / 100.0);
            row.put("riskLevel", fraudScore >= 70 ? "HIGH" : fraudScore >= 50 ? "MEDIUM" : "LOW");
            result.add(row);
        }
        return result;
    }
}
