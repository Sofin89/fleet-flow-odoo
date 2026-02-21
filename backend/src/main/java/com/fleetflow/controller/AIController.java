package com.fleetflow.controller;

import com.fleetflow.dto.ApiResponse;
import com.fleetflow.service.DriverPerformanceService;
import com.fleetflow.service.FinancialIntelligenceService;
import com.fleetflow.service.PredictiveAnalyticsService;
import com.fleetflow.security.RoleAllowed;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI features for Fleet Manager & Analyst: predictive analytics,
 * financial intelligence, driver performance scoring.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final PredictiveAnalyticsService predictiveAnalyticsService;
    private final FinancialIntelligenceService financialIntelligenceService;
    private final DriverPerformanceService driverPerformanceService;

    @GetMapping("/predictive")
    @RoleAllowed({"ROLE_MANAGER", "ROLE_ANALYST"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPredictiveAnalytics() {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("maintenanceForecast30Days", predictiveAnalyticsService.getMaintenanceForecast30Days());
        data.put("demandForecastNextWeek", predictiveAnalyticsService.getDemandForecastNextWeek());
        data.put("revenueProjection", predictiveAnalyticsService.getRevenueProjection());
        data.put("driverRiskProbability", predictiveAnalyticsService.getDriverRiskProbability());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/financial")
    @RoleAllowed({"ROLE_MANAGER", "ROLE_ANALYST"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFinancialIntelligence() {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("costPerTrip", financialIntelligenceService.getCostPerTrip());
        data.put("fuelEfficiencyPerVehicle", financialIntelligenceService.getFuelEfficiencyPerVehicle());
        data.put("profitPerRoute", financialIntelligenceService.getProfitPerRoute());
        data.put("fraudRiskScore", financialIntelligenceService.getFraudRiskScore());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/driver-performance")
    @RoleAllowed({"ROLE_MANAGER", "ROLE_ANALYST"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDriverPerformance() {
        Map<String, Object> data = driverPerformanceService.getLeaderboard();
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
