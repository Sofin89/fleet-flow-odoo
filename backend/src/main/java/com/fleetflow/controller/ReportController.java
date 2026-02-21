package com.fleetflow.controller;

import com.fleetflow.dto.ApiResponse;
import com.fleetflow.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/fuel-efficiency")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFuelEfficiency() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getFuelEfficiency()));
    }

    @GetMapping("/vehicle-roi")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getVehicleROI() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getVehicleROI()));
    }

    @GetMapping("/operational-costs")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOperationalCosts() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getOperationalCosts()));
    }
}
