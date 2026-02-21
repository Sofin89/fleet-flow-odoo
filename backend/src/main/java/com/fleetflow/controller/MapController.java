package com.fleetflow.controller;

import com.fleetflow.dto.ApiResponse;
import com.fleetflow.dto.MapMarkerDTO;
import com.fleetflow.service.MapTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {

    private final MapTrackingService mapTrackingService;

    @GetMapping("/markers")
    public ResponseEntity<ApiResponse<List<MapMarkerDTO>>> getMapMarkers() {
        List<MapMarkerDTO> markers = mapTrackingService.getAllMapMarkers();
        return ResponseEntity.ok(ApiResponse.success("Map markers retrieved", markers));
    }
}
