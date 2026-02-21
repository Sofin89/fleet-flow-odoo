package com.fleetflow.controller;

import com.fleetflow.dto.*;
import com.fleetflow.enums.VehicleStatus;
import com.fleetflow.enums.VehicleType;
import com.fleetflow.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<VehicleResponse>>> getAll(
            @RequestParam(required = false) VehicleStatus status,
            @RequestParam(required = false) VehicleType type,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(ApiResponse.success(
                vehicleService.getAllVehicles(status, type, region, PageRequest.of(page, size, sort))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(vehicleService.getVehicle(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponse>> create(@Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Vehicle created", vehicleService.createVehicle(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> update(@PathVariable Long id, @Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Vehicle updated", vehicleService.updateVehicle(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        VehicleStatus status = VehicleStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(ApiResponse.success("Status updated", vehicleService.updateStatus(id, status)));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getAvailable() {
        return ResponseEntity.ok(ApiResponse.success(vehicleService.getAvailableVehicles()));
    }
}
