package com.fleetflow.controller;

import com.fleetflow.dto.ApiResponse;
import com.fleetflow.dto.DriverLocationResponse;
import com.fleetflow.service.DriverLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers/locations")
@RequiredArgsConstructor
public class DriverLocationController {

    private final DriverLocationService locationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DriverLocationResponse>>> getAllLocations() {
        List<DriverLocationResponse> locations = locationService.getAllLocations();
        return ResponseEntity.ok(ApiResponse.success("Driver locations retrieved", locations));
    }
}
