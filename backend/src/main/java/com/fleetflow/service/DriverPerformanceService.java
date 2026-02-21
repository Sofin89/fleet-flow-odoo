package com.fleetflow.service;

import com.fleetflow.entity.Driver;
import com.fleetflow.entity.Trip;
import com.fleetflow.repository.DriverRepository;
import com.fleetflow.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Driver performance scoring: leaderboard with safety, efficiency, on-time %, risk level;
 * top performer badge; high-risk alert for HR decisions.
 */
@Service
@RequiredArgsConstructor
public class DriverPerformanceService {

    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;

    /**
     * Leaderboard: Driver | Safety Score | Efficiency | On-time % | Risk Level
     * Plus topPerformerId and highRiskDriverIds.
     */
    public Map<String, Object> getLeaderboard() {
        List<Driver> drivers = driverRepository.findAll();
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> rows = new ArrayList<>();
        Long topPerformerId = null;
        List<Long> highRiskDriverIds = new ArrayList<>();
        double maxScore = -1;

        for (Driver d : drivers) {
            int safety = d.getSafetyScore() != null ? d.getSafetyScore() : 100;
            double completionRate = d.getTripCompletionRate() != null ? d.getTripCompletionRate() : 0;
            long completed = tripRepository.countCompletedByDriver(d.getId());
            long total = tripRepository.countFinishedByDriver(d.getId());
            double onTimePercent = total > 0 ? (completed * 100.0 / total) : 0;

            long daysToExpiry = d.getLicenseExpiry() != null
                    ? ChronoUnit.DAYS.between(today, d.getLicenseExpiry())
                    : 365;
            double risk = 100 - (safety * 0.5 + completionRate * 30 + (daysToExpiry > 30 ? 20 : daysToExpiry > 0 ? 10 : 0));
            risk = Math.max(0, Math.min(100, risk));
            String riskLevel = risk >= 70 ? "HIGH" : risk >= 40 ? "MEDIUM" : "LOW";

            if (risk >= 70) highRiskDriverIds.add(d.getId());

            // Efficiency: composite of completion rate and safety (simplified)
            double efficiency = (safety / 100.0) * 0.5 + (completionRate) * 0.5;
            efficiency = Math.round(efficiency * 100.0) / 100.0;

            double leaderboardScore = safety * 0.4 + efficiency * 100 * 0.3 + onTimePercent * 0.3;
            if (leaderboardScore > maxScore) {
                maxScore = leaderboardScore;
                topPerformerId = d.getId();
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("driverId", d.getId());
            row.put("driverName", d.getFullName());
            row.put("safetyScore", safety);
            row.put("efficiency", efficiency);
            row.put("onTimePercent", Math.round(onTimePercent * 100.0) / 100.0);
            row.put("riskLevel", riskLevel);
            row.put("completedTrips", completed);
            row.put("totalTrips", total);
            rows.add(row);
        }

        // Sort by safety then efficiency (desc)
        rows.sort((a, b) -> {
            int s = Integer.compare((Integer) b.get("safetyScore"), (Integer) a.get("safetyScore"));
            if (s != 0) return s;
            return Double.compare((Double) b.get("efficiency"), (Double) a.get("efficiency"));
        });

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("leaderboard", rows);
        out.put("topPerformerId", topPerformerId);
        out.put("highRiskDriverIds", highRiskDriverIds);
        return out;
    }
}
