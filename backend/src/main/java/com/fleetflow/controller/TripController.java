package com.fleetflow.controller;

import com.fleetflow.dto.*;
import com.fleetflow.entity.Driver;
import com.fleetflow.entity.User;
import com.fleetflow.enums.Role;
import com.fleetflow.enums.TripStatus;
import com.fleetflow.exception.BusinessException;
import com.fleetflow.repository.DriverRepository;
import com.fleetflow.repository.UserRepository;
import com.fleetflow.security.RoleAllowed;
import com.fleetflow.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TripResponse>>> getAll(
            @RequestParam(required = false) TripStatus status,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) Long driverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        
        // Get authenticated user
        User user = getAuthenticatedUser();
        
        // Apply role-based filtering
        // Requirements: 8.1, 8.2, 8.3
        if (user.getRole() == Role.DRIVER) {
            // DRIVER role: Filter to only trips assigned to this driver
            Long authenticatedDriverId = getAuthenticatedDriverId();
            Page<TripResponse> trips = tripService.getAllTrips(status, vehicleId, authenticatedDriverId, PageRequest.of(page, size, sort));
            return ResponseEntity.ok(ApiResponse.success(trips));
        } else {
            // MANAGER, DISPATCHER, SAFETY_OFFICER, ANALYST: Return all trips
            Page<TripResponse> trips = tripService.getAllTrips(status, vehicleId, driverId, PageRequest.of(page, size, sort));
            return ResponseEntity.ok(ApiResponse.success(trips));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TripResponse>> getById(@PathVariable Long id) {
        // Get the trip first
        TripResponse trip = tripService.getTrip(id);
        
        // Check if user is authorized to view this trip
        // Requirements: 8.5, 14.2
        User user = getAuthenticatedUser();
        
        if (user.getRole() == Role.DRIVER) {
            // Drivers can only view their own trips
            Long authenticatedDriverId = getAuthenticatedDriverId();
            if (!trip.getDriverId().equals(authenticatedDriverId)) {
                throw new BusinessException("You do not have permission to view this trip");
            }
        }
        // Other roles (MANAGER, DISPATCHER, SAFETY_OFFICER, ANALYST) can view all trips
        
        return ResponseEntity.ok(ApiResponse.success(trip));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TripResponse>> create(@Valid @RequestBody TripRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Trip created", tripService.createTrip(request)));
    }

    @PutMapping("/{id}")
    @RoleAllowed({"ROLE_MANAGER", "ROLE_DISPATCHER"})
    public ResponseEntity<ApiResponse<TripResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TripRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Trip updated", tripService.updateTrip(id, request)));
    }

    @PatchMapping("/{id}/dispatch")
    public ResponseEntity<ApiResponse<TripResponse>> dispatch(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Trip dispatched", tripService.dispatchTrip(id)));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<TripResponse>> complete(@PathVariable Long id,
                                                              @RequestBody(required = false) TripCompleteRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Trip completed", tripService.completeTrip(id, request)));
    }

    @PatchMapping("/{id}/cancel")
    @RoleAllowed({"ROLE_MANAGER", "ROLE_DISPATCHER"})
    public ResponseEntity<ApiResponse<TripResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Trip cancelled", tripService.cancelTrip(id)));
    }

    /**
     * Get the authenticated user from the security context.
     * 
     * @return User entity
     * @throws BusinessException if user is not found
     */
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));
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
        
        // Find driver by email
        Driver driver = driverRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Driver profile not found for this user"));
        
        return driver.getId();
    }
}
