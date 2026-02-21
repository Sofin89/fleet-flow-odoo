package com.fleetflow.controller;

import com.fleetflow.dto.ApiResponse;
import com.fleetflow.dto.DriverLocationResponse;
import com.fleetflow.dto.LocationHistoryResponse;
import com.fleetflow.dto.LocationUpdateRequest;
import com.fleetflow.entity.Driver;
import com.fleetflow.entity.User;
import com.fleetflow.enums.Role;
import com.fleetflow.exception.BusinessException;
import com.fleetflow.repository.DriverRepository;
import com.fleetflow.repository.UserRepository;
import com.fleetflow.security.RoleAllowed;
import com.fleetflow.service.DriverLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for driver location operations.
 * Provides endpoints for starting/stopping location sharing, updating locations,
 * and retrieving location data with role-based access control.
 * 
 * Requirements: 1.1, 2.2, 2.4, 3.1, 3.5, 11.4
 */
@RestController
@RequestMapping("/api/drivers/locations")
@RequiredArgsConstructor
public class DriverLocationController {

    private final DriverLocationService locationService;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;

    /**
     * Start location sharing for the authenticated driver.
     * Only accessible by users with DRIVER role.
     * 
     * @param request Location update request with initial coordinates
     * @return Response with driver location data
     * Requirements: 1.1, 2.2
     */
    @PostMapping("/start")
    @RoleAllowed("ROLE_DRIVER")
    public ResponseEntity<ApiResponse<DriverLocationResponse>> startSharing(
            @Valid @RequestBody LocationUpdateRequest request) {
        
        Long driverId = getAuthenticatedDriverId();
        DriverLocationResponse response = locationService.startLocationSharing(driverId, request);
        return ResponseEntity.ok(ApiResponse.success("Location sharing started", response));
    }

    /**
     * Stop location sharing for the authenticated driver.
     * Only accessible by users with DRIVER role.
     * 
     * @return Success response
     * Requirements: 2.4
     */
    @PostMapping("/stop")
    @RoleAllowed("ROLE_DRIVER")
    public ResponseEntity<ApiResponse<Void>> stopSharing() {
        Long driverId = getAuthenticatedDriverId();
        locationService.stopLocationSharing(driverId);
        return ResponseEntity.ok(ApiResponse.success("Location sharing stopped", null));
    }

    /**
     * Update location for the authenticated driver.
     * Only accessible by users with DRIVER role.
     * 
     * @param request Location update request with new coordinates
     * @return Response with updated driver location data
     * Requirements: 3.1, 3.5
     */
    @PutMapping
    @RoleAllowed("ROLE_DRIVER")
    public ResponseEntity<ApiResponse<DriverLocationResponse>> updateLocation(
            @Valid @RequestBody LocationUpdateRequest request) {
        
        Long driverId = getAuthenticatedDriverId();
        DriverLocationResponse response = locationService.updateLocation(driverId, request);
        return ResponseEntity.ok(ApiResponse.success("Location updated", response));
    }

    /**
     * Get all active driver locations.
     * Accessible by MANAGER, DISPATCHER, SAFETY_OFFICER, and ANALYST roles.
     * 
     * @return List of all active driver locations
     * Requirements: 3.1
     */
    @GetMapping
    @RoleAllowed({"ROLE_MANAGER", "ROLE_DISPATCHER", "ROLE_SAFETY_OFFICER", "ROLE_ANALYST"})
    public ResponseEntity<ApiResponse<List<DriverLocationResponse>>> getAllLocations() {
        List<DriverLocationResponse> locations = locationService.getAllActiveLocations();
        return ResponseEntity.ok(ApiResponse.success("Driver locations retrieved", locations));
    }

    /**
     * Get location for a specific driver.
     * Accessible by MANAGER, DISPATCHER, SAFETY_OFFICER, ANALYST, or the driver themselves.
     * 
     * @param driverId ID of the driver
     * @return Driver location data
     * Requirements: 3.1
     */
    @GetMapping("/{driverId}")
    public ResponseEntity<ApiResponse<DriverLocationResponse>> getDriverLocation(
            @PathVariable Long driverId) {
        
        // Check if user is authorized (has privileged role or is requesting their own location)
        if (!isAuthorizedToViewDriver(driverId)) {
            throw new BusinessException("You do not have permission to view this driver's location");
        }
        
        DriverLocationResponse response = locationService.getDriverLocation(driverId);
        return ResponseEntity.ok(ApiResponse.success("Driver location retrieved", response));
    }

    /**
     * Get location history for a specific driver within a date range.
     * Accessible by MANAGER, DISPATCHER, SAFETY_OFFICER, ANALYST, or the driver themselves.
     * 
     * @param driverId ID of the driver
     * @param startDate Start date of the history range
     * @param endDate End date of the history range
     * @return List of location history records
     * Requirements: 11.4
     */
    @GetMapping("/{driverId}/history")
    public ResponseEntity<ApiResponse<List<LocationHistoryResponse>>> getLocationHistory(
            @PathVariable Long driverId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        // Check if user is authorized (has privileged role or is requesting their own history)
        if (!isAuthorizedToViewDriver(driverId)) {
            throw new BusinessException("You do not have permission to view this driver's location history");
        }
        
        List<LocationHistoryResponse> history = locationService.getLocationHistory(driverId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Location history retrieved", history));
    }

    /**
     * Get the authenticated driver's ID from the security context.
     * 
     * @return Driver ID
     * @throws BusinessException if user is not found or is not a driver
     */
    private Long getAuthenticatedDriverId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));
        
        // Find driver by email
        Driver driver = driverRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Driver profile not found for this user"));
        
        return driver.getId();
    }

    /**
     * Check if the authenticated user is authorized to view a specific driver's data.
     * Users are authorized if they have a privileged role (MANAGER, DISPATCHER, SAFETY_OFFICER, ANALYST)
     * or if they are requesting their own data.
     * 
     * @param driverId ID of the driver to check
     * @return true if authorized, false otherwise
     */
    private boolean isAuthorizedToViewDriver(Long driverId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));
        
        // Check if user has privileged role
        Role role = user.getRole();
        if (role == Role.MANAGER || role == Role.DISPATCHER || 
            role == Role.SAFETY_OFFICER || role == Role.ANALYST) {
            return true;
        }
        
        // Check if user is requesting their own data
        if (role == Role.DRIVER) {
            Driver driver = driverRepository.findByEmail(email).orElse(null);
            return driver != null && driver.getId().equals(driverId);
        }
        
        return false;
    }
}
