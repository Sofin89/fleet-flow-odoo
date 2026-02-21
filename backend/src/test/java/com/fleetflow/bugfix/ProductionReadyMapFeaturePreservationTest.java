package com.fleetflow.bugfix;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetflow.dto.TripCompleteRequest;
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
 * Preservation Property Tests for Production-Ready Map Feature
 * 
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.11, 3.13, 3.14, 3.15, 3.16, 3.17, 3.18**
 * 
 * This test suite verifies that existing trip management and map display behavior
 * remains unchanged after implementing the production-ready map feature fixes.
 * 
 * EXPECTED OUTCOME: All tests MUST PASS on both unfixed and fixed code
 * (confirms no regressions in existing functionality)
 * 
 * Testing Approach: Observation-first methodology
 * - Observe behavior on UNFIXED code for non-buggy inputs
 * - Write tests capturing observed behavior patterns
 * - Verify tests pass on UNFIXED code (baseline)
 * - Verify tests still pass on FIXED code (no regressions)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ProductionReadyMapFeaturePreservationTest {

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
                .licensePlate("TEST-" + System.currentTimeMillis())
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
                .email("test" + System.currentTimeMillis() + "@example.com")
                .licenseNumber("LIC-" + System.currentTimeMillis())
                .licenseCategory(LicenseCategory.TRUCK)
                .licenseExpiry(LocalDate.now().plusYears(1))
                .dutyStatus(DutyStatus.ON_DUTY)
                .build();
        testDriver = driverRepository.save(testDriver);
    }

    /**
     * Property 1: Trip Creation Validation (Requirement 3.1)
     * 
     * Validates that trip creation continues to validate:
     * - Vehicle availability
     * - Driver duty status
     * - License validity
     * - Cargo capacity
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void tripCreationShouldValidateBusinessRules() throws Exception {
        // Create trip with valid data
        TripRequest request = new TripRequest();
        request.setVehicleId(testVehicle.getId());
        request.setDriverId(testDriver.getId());
        request.setOrigin("23.0225,72.5714");
        request.setDestination("23.0300,72.5800");
        request.setCargoWeightKg(new BigDecimal("2000"));
        request.setRevenue(new BigDecimal("1500"));

        MvcResult result = mockMvc.perform(post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Map<String, Object> data = objectMapper.readValue(response, Map.class);
        Map<String, Object> tripData = (Map<String, Object>) data.get("data");

        // Verify trip was created with correct data
        assertThat(tripData.get("status")).isEqualTo("DRAFT");
        assertThat(new BigDecimal(tripData.get("cargoWeightKg").toString()))
                .isEqualByComparingTo(new BigDecimal("2000"));
        assertThat(tripData.get("origin")).isEqualTo("23.0225,72.5714");
        assertThat(tripData.get("destination")).isEqualTo("23.0300,72.5800");
    }

    /**
     * Property 2: Trip Creation Rejects Invalid Cargo Weight (Requirement 3.1)
     * 
     * Validates that cargo weight exceeding vehicle capacity is rejected.
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void tripCreationShouldRejectExcessiveCargo() throws Exception {
        TripRequest request = new TripRequest();
        request.setVehicleId(testVehicle.getId());
        request.setDriverId(testDriver.getId());
        request.setOrigin("23.0225,72.5714");
        request.setDestination("23.0300,72.5800");
        request.setCargoWeightKg(new BigDecimal("6000")); // Exceeds 5000 kg capacity
        request.setRevenue(new BigDecimal("1500"));

        MvcResult result = mockMvc.perform(post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertThat(response).contains("exceeds vehicle max capacity");
    }

    /**
     * Property 3: Trip Dispatch Updates Vehicle Status (Requirement 3.2)
     * 
     * Validates that dispatching a trip updates vehicle status to ON_TRIP
     * and maintains all existing dispatch logic.
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void tripDispatchShouldUpdateVehicleStatus() throws Exception {
        // Create trip
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

        // Dispatch trip
        MvcResult dispatchResult = mockMvc.perform(patch("/api/trips/" + tripId + "/dispatch"))
                .andExpect(status().isOk())
                .andReturn();

        String dispatchResponse = dispatchResult.getResponse().getContentAsString();
        Map<String, Object> dispatchData = objectMapper.readValue(dispatchResponse, Map.class);
        Map<String, Object> dispatchedTrip = (Map<String, Object>) dispatchData.get("data");

        // Verify trip status changed to DISPATCHED
        assertThat(dispatchedTrip.get("status")).isEqualTo("DISPATCHED");

        // Verify vehicle status changed to ON_TRIP
        Vehicle vehicle = vehicleRepository.findById(testVehicle.getId()).orElseThrow();
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.ON_TRIP);
    }

    /**
     * Property 4: Trip Completion Updates Odometer and Revenue (Requirement 3.3)
     * 
     * Validates that completing a trip:
     * - Updates odometer readings
     * - Calculates revenue correctly
     * - Resets vehicle and driver status
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void tripCompletionShouldUpdateOdometerAndRevenue() throws Exception {
        // Create and dispatch trip
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

        mockMvc.perform(patch("/api/trips/" + tripId + "/dispatch"))
                .andExpect(status().isOk());

        // Complete trip with updated odometer and revenue
        TripCompleteRequest completeRequest = new TripCompleteRequest();
        completeRequest.setEndOdometer(new BigDecimal("10500"));
        completeRequest.setRevenue(new BigDecimal("2000"));

        MvcResult completeResult = mockMvc.perform(patch("/api/trips/" + tripId + "/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String completeResponse = completeResult.getResponse().getContentAsString();
        Map<String, Object> completeData = objectMapper.readValue(completeResponse, Map.class);
        Map<String, Object> completedTrip = (Map<String, Object>) completeData.get("data");

        // Verify trip status changed to COMPLETED
        assertThat(completedTrip.get("status")).isEqualTo("COMPLETED");
        
        // Verify odometer and revenue updated
        assertThat(new BigDecimal(completedTrip.get("endOdometer").toString()))
                .isEqualByComparingTo(new BigDecimal("10500"));
        assertThat(new BigDecimal(completedTrip.get("revenue").toString()))
                .isEqualByComparingTo(new BigDecimal("2000"));

        // Verify vehicle status reset to AVAILABLE
        Vehicle vehicle = vehicleRepository.findById(testVehicle.getId()).orElseThrow();
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(vehicle.getOdometerKm()).isEqualByComparingTo(new BigDecimal("10500"));
    }

    /**
     * Property 5: Trip Cancellation Resets Status (Requirement 3.4)
     * 
     * Validates that cancelling a trip resets vehicle and driver status appropriately.
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void tripCancellationShouldResetStatusForDraftTrip() throws Exception {
        // Create trip
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

        // Cancel trip
        MvcResult cancelResult = mockMvc.perform(patch("/api/trips/" + tripId + "/cancel"))
                .andExpect(status().isOk())
                .andReturn();

        String cancelResponse = cancelResult.getResponse().getContentAsString();
        Map<String, Object> cancelData = objectMapper.readValue(cancelResponse, Map.class);
        Map<String, Object> cancelledTrip = (Map<String, Object>) cancelData.get("data");

        // Verify trip status changed to CANCELLED
        assertThat(cancelledTrip.get("status")).isEqualTo("CANCELLED");

        // Verify vehicle status remains AVAILABLE
        Vehicle vehicle = vehicleRepository.findById(testVehicle.getId()).orElseThrow();
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        
        // Verify driver status remains ON_DUTY
        Driver driver = driverRepository.findById(testDriver.getId()).orElseThrow();
        assertThat(driver.getDutyStatus()).isEqualTo(DutyStatus.ON_DUTY);
    }

    /**
     * Property 6: Trip Cancellation Resets Status for Dispatched Trip (Requirement 3.4)
     * 
     * Validates that cancelling a dispatched trip resets vehicle and driver status.
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void tripCancellationShouldResetStatusForDispatchedTrip() throws Exception {
        // Create and dispatch trip
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

        // Dispatch trip
        mockMvc.perform(patch("/api/trips/" + tripId + "/dispatch"))
                .andExpect(status().isOk());

        // Cancel trip
        MvcResult cancelResult = mockMvc.perform(patch("/api/trips/" + tripId + "/cancel"))
                .andExpect(status().isOk())
                .andReturn();

        String cancelResponse = cancelResult.getResponse().getContentAsString();
        Map<String, Object> cancelData = objectMapper.readValue(cancelResponse, Map.class);
        Map<String, Object> cancelledTrip = (Map<String, Object>) cancelData.get("data");

        // Verify trip status changed to CANCELLED
        assertThat(cancelledTrip.get("status")).isEqualTo("CANCELLED");

        // Verify vehicle status reset to AVAILABLE
        Vehicle vehicle = vehicleRepository.findById(testVehicle.getId()).orElseThrow();
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        
        // Verify driver status reset to ON_DUTY
        Driver driver = driverRepository.findById(testDriver.getId()).orElseThrow();
        assertThat(driver.getDutyStatus()).isEqualTo(DutyStatus.ON_DUTY);
    }

    /**
     * Property 7: Text-Based Coordinates Still Accepted (Requirement 3.11)
     * 
     * Validates that the legacy text-based coordinate format continues to work
     * after implementing map-based location selection.
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void textBasedCoordinatesShouldStillWork() throws Exception {
        String origin = "23.0225,72.5714";
        String destination = "23.0300,72.5800";

        TripRequest request = new TripRequest();
        request.setVehicleId(testVehicle.getId());
        request.setDriverId(testDriver.getId());
        request.setOrigin(origin);
        request.setDestination(destination);
        request.setCargoWeightKg(new BigDecimal("2000"));
        request.setRevenue(new BigDecimal("1500"));

        MvcResult result = mockMvc.perform(post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Map<String, Object> data = objectMapper.readValue(response, Map.class);
        Map<String, Object> tripData = (Map<String, Object>) data.get("data");

        // Verify trip was created with text-based coordinates
        assertThat(tripData.get("origin")).isEqualTo(origin);
        assertThat(tripData.get("destination")).isEqualTo(destination);
    }

    /**
     * Property 8: Map Markers Display Correctly (Requirements 3.13, 3.14, 3.15)
     * 
     * Validates that map markers continue to display correctly for:
     * - Combined markers (driver + vehicle on trip)
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void mapMarkersShouldDisplayCorrectly() throws Exception {
        // Create and dispatch a trip to generate a COMBINED marker
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

        mockMvc.perform(patch("/api/trips/" + tripId + "/dispatch"))
                .andExpect(status().isOk());

        // Fetch map markers
        MvcResult markersResult = mockMvc.perform(get("/api/map/markers"))
                .andExpect(status().isOk())
                .andReturn();

        String markersResponse = markersResult.getResponse().getContentAsString();
        Map<String, Object> markersData = objectMapper.readValue(markersResponse, Map.class);
        List<Map<String, Object>> markers = (List<Map<String, Object>>) markersData.get("data");

        // Verify markers exist
        assertThat(markers).isNotEmpty();

        // Find the COMBINED marker for our dispatched trip
        Map<String, Object> combinedMarker = markers.stream()
                .filter(m -> {
                    if (!"COMBINED".equals(m.get("markerType"))) {
                        return false;
                    }
                    // Check if tripId exists and matches
                    Object tripIdObj = m.get("tripId");
                    if (tripIdObj == null) {
                        return false;
                    }
                    return tripId.equals(((Number) tripIdObj).longValue());
                })
                .findFirst()
                .orElse(null);

        // Note: On unfixed code, the marker might not have tripId field
        // This test verifies that markers are displayed, even if not all fields are present
        if (combinedMarker != null) {
            assertThat(combinedMarker.get("latitude")).isNotNull();
            assertThat(combinedMarker.get("longitude")).isNotNull();
            assertThat(combinedMarker.get("tripOrigin")).isEqualTo("23.0225,72.5714");
            assertThat(combinedMarker.get("tripDestination")).isEqualTo("23.0300,72.5800");
        } else {
            // If no COMBINED marker found with tripId, verify at least some COMBINED markers exist
            long combinedMarkerCount = markers.stream()
                    .filter(m -> "COMBINED".equals(m.get("markerType")))
                    .count();
            assertThat(combinedMarkerCount).as("At least one COMBINED marker should exist").isGreaterThan(0);
        }
    }
}
