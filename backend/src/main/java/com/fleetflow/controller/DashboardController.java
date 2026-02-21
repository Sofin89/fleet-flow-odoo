package com.fleetflow.controller;

import com.fleetflow.dto.ApiResponse;
import com.fleetflow.dto.DashboardKPI;
import com.fleetflow.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/kpis")
    public ResponseEntity<ApiResponse<DashboardKPI>> getKPIs() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getKPIs()));
    }
}
