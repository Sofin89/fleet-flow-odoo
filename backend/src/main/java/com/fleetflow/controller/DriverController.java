package com.fleetflow.controller;

import com.fleetflow.dto.*;
import com.fleetflow.enums.DutyStatus;
import com.fleetflow.enums.LicenseCategory;
import com.fleetflow.service.DriverService;
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
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DriverResponse>>> getAll(
            @RequestParam(required = false) DutyStatus status,
            @RequestParam(required = false) LicenseCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(ApiResponse.success(
                driverService.getAllDrivers(status, category, PageRequest.of(page, size, sort))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(driverService.getDriver(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DriverResponse>> create(@Valid @RequestBody DriverRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Driver created", driverService.createDriver(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverResponse>> update(@PathVariable Long id, @Valid @RequestBody DriverRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Driver updated", driverService.updateDriver(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<DriverResponse>> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        DutyStatus status = DutyStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(ApiResponse.success("Status updated", driverService.updateStatus(id, status)));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<DriverResponse>>> getAvailable(
            @RequestParam(required = false) LicenseCategory category) {
        return ResponseEntity.ok(ApiResponse.success(driverService.getAvailableDrivers(category)));
    }
}
