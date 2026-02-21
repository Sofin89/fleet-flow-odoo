package com.fleetflow.controller;

import com.fleetflow.dto.*;
import com.fleetflow.service.MaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<MaintenanceResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                maintenanceService.getAllLogs(PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<ApiResponse<Page<MaintenanceResponse>>> getByVehicle(
            @PathVariable Long vehicleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                maintenanceService.getLogsByVehicle(vehicleId, PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MaintenanceResponse>> create(@Valid @RequestBody MaintenanceRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Maintenance log created", maintenanceService.createLog(request)));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> complete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Maintenance completed", maintenanceService.completeMaintenance(id)));
    }
}
