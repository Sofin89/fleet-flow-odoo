package com.fleetflow.bugfix;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetflow.dto.TripRequest;
import com.fleetflow.entity.Driver;
import com.fleetflow.entity.Vehicle;
import com.fleetflow.enums.*;
import com.fleetflow.repository.DriverRepository;
import com.fleetflow.repository.TripRepository;
import com.fleetflow.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Bug Condition Exploration Test for Production-Ready Map Feature
 * 
 * **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10**
 * 
 * This test verifies that three critical bugs exist in the unfixed codebase:
 * 1. Trip Update Limitation: No endpoint to update DRAFT trips
 * 2. Route Display Deficiency: Routes display as straight lines (2 points only)
 * 3. Trip Creation UX Limitation: No map-based location selection component
 * 
 * EXPECTED OUTCOME: This test MUST FAIL on unfixed code (failure confirms bugs exist)
 * 
 * After the fix is implemented, this same test should PASS, confirming:
 * - Trip update endpoint exists and works for DRAFT trips (Requirements 2.1, 2.2)
 * - Routes follow road networks with >2 points (Requirements 2.10, 2.13)
 * - Map picker component is available (Requirements 2.5, 2.6)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ProductionReadyMapFeatureBugExplorationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private TripRepository tripRepository;

    private Vehicle testVehicle;
    private Driver testDriver;

    @BeforeEach
    public void setup() {
        // Create test vehicle
        testVehicle = Vehicle.builder()
                .name("Test Truck")
                .model("Test Model")
                .licensePlate("TEST-001")
                .vehicleType(VehicleType.TRUCK)
                .maxLoadCapacityKg(new BigDecimal("5000"))
                .odometerKm(new BigDecimal("10000"))
                .status(VehicleStatus.AVAILABLE)
                .build();
        testVehicle = vehicleRepository.save(testVehicle);

        // Create test driver
        testDriver = Driver.builder()
                .fullName("Test Driver")
                .phone("1234567890")
                .email("test@example.com")
                .licenseNumber("LIC-001")
                .licenseCategory(LicenseCategory.TRUCK)
                .licenseExpiry(LocalDate.now().plusYears(1))
                .dutyStatus(DutyStatus.ON_DUTY)
                .build();
        testDriver = driverRepository.save(testDriver);
    }

    /**
     * Bug 1: Trip Update Endpoint Deficiency
     * 
     * Bug Condition: isBugCondition(trip) where trip.status = DRAFT AND no update endpoint exists
     * Expected Behavior: PUT /api/trips/{id} should exist and allow updating DRAFT trips
     * 
     * This test verifies:
     * - ON UNFIXED CODE: Endpoint returns 404 or 405 (not found/not allowed)
     * - ON FIXED CODE: Endpoint returns 200 and updates the trip successfully
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void tripUpdateEndpointShouldExistForDraftTrips() throws Exception {
        // Create a DRAFT trip
        TripRequest createRequest = new TripRequest();
        createRequest.setVehicleId(testVehicle.getId());
        createRequest.setDriverId(testDriver.getId());
        createRequest.setOrigin("23.0225,72.5714");
        createRequest.setDestination("23.0300,72.5800");
        createRequest.setCargoWeightKg(new BigDecimal("2000"));
        createRequest.setRevenue(new BigDecimal("1500"));

        MvcResult createResult = mockMvc.perform(post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        Map<String, Object> createData = objectMapper.readValue(createResponse, Map.class);
        Map<String, Object> tripData = (Map<String, Object>) createData.get("data");
        Long tripId = ((Number) tripData.get("id")).longValue();

        // Attempt to update the DRAFT trip
        TripRequest updateRequest = new TripRequest();
        updateRequest.setVehicleId(testVehicle.getId());
        updateRequest.setDriverId(testDriver.getId());
        updateRequest.setOrigin("23.0500,72.6000");
        updateRequest.setDestination("23.0800,72.6500");
        updateRequest.setCargoWeightKg(new BigDecimal("2500"));
        updateRequest.setRevenue(new BigDecimal("2000"));

        // EXPECTED ON UNFIXED CODE: 404 or 405 (endpoint doesn't exist)
        // EXPECTED ON FIXED CODE: 200 (update succeeds)
        MvcResult updateResult = mockMvc.perform(put("/api/trips/" + tripId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andReturn();

        int status = updateResult.getResponse().getStatus();
        
        // Expected behavior after fix: status should be 200
        // On unfixed code: status will be 404 or 405
        assertThat(status).as("Trip update endpoint should exist and return 200 for DRAFT trips")
                .isEqualTo(200);

        // Verify the trip was actually updated
        if (status == 200) {
            String updateResponse = updateResult.getResponse().getContentAsString();
            Map<String, Object> updateData = objectMapper.readValue(updateResponse, Map.class);
            Map<String, Object> updatedTrip = (Map<String, Object>) updateData.get("data");
            
            assertThat(updatedTrip.get("origin")).isEqualTo("23.0500,72.6000");
            assertThat(updatedTrip.get("destination")).isEqualTo("23.0800,72.6500");
            assertThat(new BigDecimal(updatedTrip.get("cargoWeightKg").toString()))
                    .isEqualByComparingTo(new BigDecimal("2500"));
        }
    }

    /**
     * Bug 2: Route Display Deficiency
     * 
     * Bug Condition: isBugCondition(route) where route is straight line (2 points) OR missing OSRM data
     * Expected Behavior: Routes should follow road network with >2 polyline points
     * 
     * This test verifies:
     * - ON UNFIXED CODE: routePolyline is null, empty, or contains only 2 points (straight line)
     * - ON FIXED CODE: routePolyline contains >2 points (road-following route)
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void routesShouldFollowRoadNetworkNotStraightLines() throws Exception {
        // Create and dispatch a trip
        TripRequest createRequest = new TripRequest();
        createRequest.setVehicleId(testVehicle.getId());
        createRequest.setDriverId(testDriver.getId());
        createRequest.setOrigin("23.0225,72.5714");
        createRequest.setDestination("23.0300,72.5800");
        createRequest.setCargoWeightKg(new BigDecimal("2000"));
        createRequest.setRevenue(new BigDecimal("1500"));

        MvcResult createResult = mockMvc.perform(post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        Map<String, Object> createData = objectMapper.readValue(createResponse, Map.class);
        Map<String, Object> tripData = (Map<String, Object>) createData.get("data");
        Long tripId = ((Number) tripData.get("id")).longValue();

        // Dispatch the trip
        mockMvc.perform(patch("/api/trips/" + tripId + "/dispatch"))
                .andExpect(status().isOk());

        // Give the system a moment to process the dispatch
        Thread.sleep(100);

        // Fetch map markers to check route data
        MvcResult markersResult = mockMvc.perform(get("/api/map/markers"))
                .andExpect(status().isOk())
                .andReturn();

        String markersResponse = markersResult.getResponse().getContentAsString();
        Map<String, Object> markersData = objectMapper.readValue(markersResponse, Map.class);
        List<Map<String, Object>> markers = (List<Map<String, Object>>) markersData.get("data");

        // Find the marker for our dispatched trip (match by vehicle and driver IDs)
        Map<String, Object> tripMarker = markers.stream()
                .filter(m -> "COMBINED".equals(m.get("markerType")) && 
                            m.get("vehicleId") != null &&
                            m.get("driverId") != null &&
                            testVehicle.getId().equals(((Number) m.get("vehicleId")).longValue()) &&
                            testDriver.getId().equals(((Number) m.get("driverId")).longValue()))
                .findFirst()
                .orElse(null);

        assertThat(tripMarker).as("Dispatched trip should appear in map markers (vehicleId=%d, driverId=%d)", 
                testVehicle.getId(), testDriver.getId()).isNotNull();

        // Check route polyline
        Object routePolyline = tripMarker.get("routePolyline");
        
        // Expected behavior after fix: routePolyline should exist and have >2 points
        // On unfixed code: routePolyline will be null, empty, or have only 2 points
        assertThat(routePolyline).as("Route polyline should exist").isNotNull();
        
        if (routePolyline instanceof List) {
            List<?> polylinePoints = (List<?>) routePolyline;
            assertThat(polylinePoints.size())
                    .as("Route should follow road network with >2 points, not straight line")
                    .isGreaterThan(2);
        }
    }

    /**
     * Bug 3: Map Location Picker Component Deficiency
     * 
     * Bug Condition: isBugCondition(tripCreation) where only text input available
     * Expected Behavior: MapLocationPicker component should exist in frontend
     * 
     * This test verifies the component exists by checking:
     * - ON UNFIXED CODE: Component file doesn't exist
     * - ON FIXED CODE: Component file exists and exports MapLocationPicker
     * 
     * Note: This is a structural test that checks file existence
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void mapLocationPickerComponentShouldExist() {
        // Check if MapLocationPicker component file exists
        java.io.File componentFile = new java.io.File("../frontend/src/components/MapLocationPicker.jsx");
        
        // Expected behavior after fix: component file should exist
        // On unfixed code: component file will not exist
        assertThat(componentFile.exists())
                .as("MapLocationPicker component should exist at ../frontend/src/components/MapLocationPicker.jsx")
                .isTrue();

        // If file exists, verify it contains the MapLocationPicker export
        if (componentFile.exists()) {
            try {
                String content = new String(java.nio.file.Files.readAllBytes(componentFile.toPath()));
                assertThat(content)
                        .as("MapLocationPicker component should export default function")
                        .contains("export default function MapLocationPicker");
            } catch (Exception e) {
                throw new RuntimeException("Failed to read MapLocationPicker component file", e);
            }
        }
    }
}
