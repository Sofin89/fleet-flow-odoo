package com.fleetflow.service;

import com.fleetflow.entity.MaintenanceLog;
import com.fleetflow.entity.Trip;
import com.fleetflow.entity.Vehicle;
import com.fleetflow.enums.TripStatus;
import com.fleetflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Rule-based predictive analytics for admin view: maintenance forecast,
 * demand forecast, revenue projection, driver risk probability.
 */
@Service
@RequiredArgsConstructor
public class PredictiveAnalyticsService {

    private static final int MAINTENANCE_CYCLE_DAYS = 90;
    private static final int FORECAST_DAYS = 30;
    private static final int DEMAND_FORECAST_DAYS = 7;

    private final VehicleRepository vehicleRepository;
    private final TripRepository tripRepository;
    private final MaintenanceLogRepository maintenanceLogRepository;
    private final DriverRepository driverRepository;
    private final FuelLogRepository fuelLogRepository;

    /**
     * Maintenance forecast: vehicles likely to need maintenance in the next 30 days
     * based on last service date + 90-day cycle.
     */
    public List<Map<String, Object>> getMaintenanceForecast30Days() {
        LocalDate now = LocalDate.now();
        LocalDate end = now.plusDays(FORECAST_DAYS);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Vehicle v : vehicleRepository.findAll()) {
            List<MaintenanceLog> logs = maintenanceLogRepository.findByVehicleId(v.getId());
            if (logs.isEmpty()) continue;

            MaintenanceLog latest = logs.stream()
                    .max(Comparator.comparing(MaintenanceLog::getServiceDate))
                    .orElse(null);
            if (latest == null) continue;

            LocalDate nextDue = latest.getServiceDate().plusDays(MAINTENANCE_CYCLE_DAYS);
            boolean overdue = nextDue.isBefore(now);
            boolean dueInWindow = !nextDue.isBefore(now) && !nextDue.isAfter(end);
            if (overdue || dueInWindow) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("vehicleId", v.getId());
                row.put("vehicleName", v.getName());
                row.put("licensePlate", v.getLicensePlate());
                row.put("lastServiceDate", latest.getServiceDate().toString());
                row.put("predictedDueDate", nextDue.toString());
                row.put("daysUntilDue", (int) ChronoUnit.DAYS.between(now, nextDue));
                row.put("overdue", overdue);
                result.add(row);
            }
        }
        return result;
    }

    /**
     * Demand forecast: predicted trip count for the next 7 days (based on recent weekly average).
     */
    public Map<String, Object> getDemandForecastNextWeek() {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(14);
        List<Trip> completed = tripRepository.findCompletedBetween(start, end);
        long count = completed.size();
        double avgPerDay = 14 > 0 ? (double) count / 14 : 0;
        int predictedTrips = (int) Math.round(avgPerDay * DEMAND_FORECAST_DAYS);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("predictedTripsNextWeek", Math.max(0, predictedTrips));
        out.put("basis", "Last 14 days average");
        out.put("recentCompletedTrips", count);
        out.put("averageTripsPerDay", Math.round(avgPerDay * 100.0) / 100.0);
        return out;
    }

    /**
     * Revenue projection: projected revenue for next 30 days from recent trend.
     */
    public Map<String, Object> getRevenueProjection() {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(30);
        List<Trip> completed = tripRepository.findCompletedBetween(start, end);
        BigDecimal total = completed.stream()
                .map(t -> t.getRevenue() == null ? BigDecimal.ZERO : t.getRevenue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        double avgPerDay = 30 > 0 ? total.doubleValue() / 30 : 0;
        BigDecimal projected = BigDecimal.valueOf(avgPerDay * FORECAST_DAYS).setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("projectedRevenueNext30Days", projected);
        out.put("recent30DayRevenue", total);
        out.put("averageRevenuePerDay", BigDecimal.valueOf(avgPerDay).setScale(2, RoundingMode.HALF_UP));
        return out;
    }

    /**
     * Driver risk probability: per-driver risk score (0–100) based on safety score,
     * trip completion rate, and license expiry.
     */
    public List<Map<String, Object>> getDriverRiskProbability() {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> result = new ArrayList<>();

        driverRepository.findAll().forEach(driver -> {
            int safety = driver.getSafetyScore() != null ? driver.getSafetyScore() : 100;
            double completion = driver.getTripCompletionRate() != null ? driver.getTripCompletionRate() : 0;
            long daysToExpiry = driver.getLicenseExpiry() != null
                    ? ChronoUnit.DAYS.between(today, driver.getLicenseExpiry())
                    : 365;

            // Risk: lower safety = higher risk; lower completion = higher risk; expiry soon = higher risk
            double risk = 100 - (safety * 0.5 + completion * 30 + (daysToExpiry > 30 ? 20 : daysToExpiry > 0 ? 10 : 0));
            risk = Math.max(0, Math.min(100, risk));

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("driverId", driver.getId());
            row.put("driverName", driver.getFullName());
            row.put("safetyScore", safety);
            row.put("tripCompletionRate", completion);
            row.put("licenseExpiry", driver.getLicenseExpiry() != null ? driver.getLicenseExpiry().toString() : null);
            row.put("riskProbability", Math.round(risk * 100.0) / 100.0);
            row.put("riskLevel", risk >= 70 ? "HIGH" : risk >= 40 ? "MEDIUM" : "LOW");
            result.add(row);
        });
        return result;
    }
}
