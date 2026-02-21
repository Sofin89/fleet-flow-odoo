package com.fleetflow.controller;

import com.fleetflow.dto.*;
import com.fleetflow.service.FuelLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fuel-logs")
@RequiredArgsConstructor
public class FuelLogController {

    private final FuelLogService fuelLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<FuelLogResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                fuelLogService.getAllLogs(PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<ApiResponse<Page<FuelLogResponse>>> getByVehicle(
            @PathVariable Long vehicleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                fuelLogService.getLogsByVehicle(vehicleId, PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FuelLogResponse>> create(@Valid @RequestBody FuelLogRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Fuel log created", fuelLogService.createLog(request)));
    }
}
