package com.fleetflow.controller;

import com.fleetflow.dto.*;
import com.fleetflow.enums.TripStatus;
import com.fleetflow.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

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
        return ResponseEntity.ok(ApiResponse.success(
                tripService.getAllTrips(status, vehicleId, driverId, PageRequest.of(page, size, sort))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TripResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(tripService.getTrip(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TripResponse>> create(@Valid @RequestBody TripRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Trip created", tripService.createTrip(request)));
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
    public ResponseEntity<ApiResponse<TripResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Trip cancelled", tripService.cancelTrip(id)));
    }
}
