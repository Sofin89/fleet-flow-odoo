package com.fleetflow.service;

import com.fleetflow.dto.DriverLocationResponse;
import com.fleetflow.dto.LocationHistoryResponse;
import com.fleetflow.dto.LocationUpdateRequest;
import com.fleetflow.entity.Driver;
import com.fleetflow.entity.DriverLocation;
import com.fleetflow.entity.LocationHistory;
import com.fleetflow.enums.DutyStatus;
import com.fleetflow.exception.BusinessException;
import com.fleetflow.exception.ResourceNotFoundException;
import com.fleetflow.repository.DriverLocationRepository;
import com.fleetflow.repository.DriverRepository;
import com.fleetflow.repository.LocationHistoryRepository;
import com.fleetflow.websocket.LocationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverLocationService {

    private final DriverLocationRepository locationRepo;
    private final DriverRepository driverRepo;
    private final LocationHistoryRepository historyRepo;
    private final LocationWebSocketHandler wsHandler;
    private final Random random = new Random();

    // Ahmedabad-area base coordinates
    private static final double BASE_LAT = 23.0225;
    private static final double BASE_LNG = 72.5714;
    
    // Maximum days allowed for location history queries
    private static final int MAX_HISTORY_DAYS = 30;
    
    // Retention period for location history (90 days)
    private static final int RETENTION_DAYS = 90;

    /**
     * Start location sharing for a driver.
     * Sets sharingActive=true, resets consecutiveFailures, and broadcasts status change.
     * 
     * Requirements: 2.2, 2.5
     */
    @Transactional
    public DriverLocationResponse startLocationSharing(Long driverId, LocationUpdateRequest request) {
        log.info("Starting location sharing for driver {}", driverId);
        
        // Validate driver exists
        Driver driver = driverRepo.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));
        
        // Validate coordinates
        validateCoordinates(request);
        
        // Create or update DriverLocation
        DriverLocation location = locationRepo.findById(driverId)
                .orElse(DriverLocation.builder()
                        .driver(driver)
                        .driverId(driverId)
                        .build());
        
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setAccuracy(request.getAccuracy());
        location.setSpeed(request.getSpeed() != null ? request.getSpeed() : 0.0);
        location.setHeading(request.getHeading());
        location.setSharingActive(true);
        location.setConsecutiveFailures(0);
        location.setLastError(null);
        location.setLastUpdated(LocalDateTime.now());
        
        locationRepo.save(location);
        
        // Save to history
        saveToHistory(driverId, request);
        
        // Broadcast status change
        wsHandler.broadcastSharingStatusChange(driverId, true);
        
        // Broadcast initial location
        DriverLocationResponse response = toResponse(location);
        wsHandler.broadcastLocationUpdate(response);
        
        log.info("Location sharing started for driver {}", driverId);
        return response;
    }

    /**
     * Stop location sharing for a driver.
     * Sets sharingActive=false and broadcasts status change.
     * 
     * Requirements: 2.4, 13.4
     */
    @Transactional
    public void stopLocationSharing(Long driverId) {
        log.info("Stopping location sharing for driver {}", driverId);
        
        DriverLocation location = locationRepo.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver location not found for id: " + driverId));
        
        location.setSharingActive(false);
        location.setLastUpdated(LocalDateTime.now());
        locationRepo.save(location);
        
        // Broadcast status change
        wsHandler.broadcastSharingStatusChange(driverId, false);
        
        log.info("Location sharing stopped for driver {}", driverId);
    }

    /**
     * Update location for a driver with active sharing.
     * Validates coordinates, updates location, saves to history, broadcasts update, and handles failures.
     * 
     * Requirements: 3.1, 15.1
     */
    @Transactional
    public DriverLocationResponse updateLocation(Long driverId, LocationUpdateRequest request) {
        log.debug("Updating location for driver {}", driverId);
        
        // Validate driver exists
        Driver driver = driverRepo.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));
        
        // Get location
        DriverLocation location = locationRepo.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver location not found for id: " + driverId));
        
        // Validate sharing is active
        if (!location.getSharingActive()) {
            throw new BusinessException("Location sharing is not active for this driver");
        }
        
        // Validate coordinates
        validateCoordinates(request);
        
        // Update location
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setAccuracy(request.getAccuracy());
        location.setSpeed(request.getSpeed() != null ? request.getSpeed() : 0.0);
        location.setHeading(request.getHeading());
        location.setLastUpdated(LocalDateTime.now());
        location.setConsecutiveFailures(0); // Reset on successful update
        location.setLastError(null);
        
        locationRepo.save(location);
        
        // Save to history
        saveToHistory(driverId, request);
        
        // Broadcast update
        DriverLocationResponse response = toResponse(location);
        wsHandler.broadcastLocationUpdate(response);
        
        log.debug("Location updated for driver {}", driverId);
        return response;
    }

    /**
     * Get all active driver locations (where sharingActive=true).
     * 
     * Requirements: 3.1
     */
    public List<DriverLocationResponse> getAllActiveLocations() {
        return locationRepo.findAll().stream()
                .filter(DriverLocation::getSharingActive)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get location for a specific driver.
     * 
     * Requirements: 3.1
     */
    public DriverLocationResponse getDriverLocation(Long driverId) {
        DriverLocation location = locationRepo.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver location not found for id: " + driverId));
        return toResponse(location);
    }

    /**
     * Get location history for a driver within a date range.
     * Validates that the date range does not exceed MAX_HISTORY_DAYS (30 days).
     *
     * Requirements: 11.4, 11.5
     */
    public List<LocationHistoryResponse> getLocationHistory(Long driverId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching location history for driver {} from {} to {}", driverId, startDate, endDate);

        // Validate driver exists
        driverRepo.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));

        // Validate date range
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("Start date must be before or equal to end date");
        }

        // Enforce MAX_HISTORY_DAYS constraint
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > MAX_HISTORY_DAYS) {
            throw new BusinessException("History range cannot exceed " + MAX_HISTORY_DAYS + " days");
        }

        // Convert LocalDate to LocalDateTime for query
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // Query history
        List<LocationHistory> history = historyRepo.findByDriverIdAndDateRange(driverId, startDateTime, endDateTime);

        // Convert to response DTOs
        return history.stream()
                .map(this::toHistoryResponse)
                .collect(Collectors.toList());
    }


    /**
     * Save location update to history.
     * 
     * Requirements: 11.4
     */
    private void saveToHistory(Long driverId, LocationUpdateRequest request) {
        LocationHistory history = LocationHistory.builder()
                .driverId(driverId)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .accuracy(request.getAccuracy())
                .speed(request.getSpeed())
                .heading(request.getHeading())
                .recordedAt(LocalDateTime.now())
                .build();
        historyRepo.save(history);
    }

    /**
     * Validate coordinates are within valid ranges.
     * 
     * Requirements: 15.1
     */
    private void validateCoordinates(LocationUpdateRequest request) {
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new BusinessException("Latitude and longitude are required");
        }
        
        if (request.getLatitude() < -90 || request.getLatitude() > 90) {
            throw new BusinessException("Latitude must be between -90 and 90");
        }
        
        if (request.getLongitude() < -180 || request.getLongitude() > 180) {
            throw new BusinessException("Longitude must be between -180 and 180");
        }
        
        if (request.getAccuracy() != null && request.getAccuracy() < 0) {
            throw new BusinessException("Accuracy cannot be negative");
        }
        
        if (request.getSpeed() != null && request.getSpeed() < 0) {
            throw new BusinessException("Speed cannot be negative");
        }
        
        if (request.getHeading() != null && (request.getHeading() < 0 || request.getHeading() > 360)) {
            throw new BusinessException("Heading must be between 0 and 360");
        }
    }

    public List<DriverLocationResponse> getAllLocations() {
        return locationRepo.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void initializeLocations() {
        List<Driver> drivers = driverRepo.findAll();
        for (int i = 0; i < drivers.size(); i++) {
            Driver driver = drivers.get(i);
            if (!locationRepo.existsById(driver.getId())) {
                double[] offset = generateOffset(i);
                DriverLocation loc = DriverLocation.builder()
                        .driver(driver)
                        .latitude(BASE_LAT + offset[0])
                        .longitude(BASE_LNG + offset[1])
                        .speed(driver.getDutyStatus() == DutyStatus.ON_DUTY
                                ? 10.0 + random.nextDouble() * 60.0 : 0.0)
                        .heading(random.nextDouble() * 360.0)
                        .lastUpdated(LocalDateTime.now())
                        .build();
                locationRepo.save(loc);
            }
        }
        log.info("Initialized locations for {} drivers", drivers.size());
    }

    /**
     * Scheduled task — simulates GPS position drift every 5 seconds.
     * ON_DUTY drivers move; OFF_DUTY/SUSPENDED stay still with speed 0.
     */
    @Scheduled(fixedRate = 5000)
    @Transactional
    public void simulateMovement() {
        List<DriverLocation> locations = locationRepo.findAll();
        for (DriverLocation loc : locations) {
            Driver driver = loc.getDriver();
            if (driver.getDutyStatus() == DutyStatus.ON_DUTY) {
                // Simulate movement: small random drift
                double dLat = (random.nextDouble() - 0.5) * 0.002;
                double dLng = (random.nextDouble() - 0.5) * 0.002;
                loc.setLatitude(loc.getLatitude() + dLat);
                loc.setLongitude(loc.getLongitude() + dLng);
                loc.setSpeed(Math.round((5.0 + random.nextDouble() * 65.0) * 10.0) / 10.0);
                loc.setHeading(random.nextDouble() * 360.0);
            } else {
                loc.setSpeed(0.0);
            }
            loc.setLastUpdated(LocalDateTime.now());
        }
        locationRepo.saveAll(locations);
    }
    /**
     * Scheduled purge job to delete location history records older than 90 days.
     * Runs daily at 2 AM to maintain the 90-day retention policy.
     *
     * Requirements: 13.3
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeOldLocationHistory() {
        log.info("Starting scheduled purge of location history older than {} days", RETENTION_DAYS);

        try {
            // Calculate cutoff date (90 days ago)
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(RETENTION_DAYS);

            // Delete records older than cutoff date
            int deletedCount = historyRepo.deleteByRecordedAtBefore(cutoffDate);

            log.info("Successfully purged {} location history records older than {}",
                    deletedCount, cutoffDate);
        } catch (Exception e) {
            log.error("Error during location history purge operation", e);
            // Don't rethrow - allow the scheduled job to continue on next run
        }
    }

    private double[] generateOffset(int index) {
        double[][] offsets = {
                {0.02, 0.01}, {-0.015, 0.025}, {0.035, -0.02},
                {-0.01, -0.03}, {0.005, 0.04}, {0.028, 0.015}
        };
        double[] base = offsets[index % offsets.length];
        return new double[]{
                base[0] + (random.nextDouble() - 0.5) * 0.01,
                base[1] + (random.nextDouble() - 0.5) * 0.01
        };
    }

    private DriverLocationResponse toResponse(DriverLocation loc) {
        Driver d = loc.getDriver();
        return DriverLocationResponse.builder()
                .driverId(d.getId())
                .fullName(d.getFullName())
                .licenseCategory(d.getLicenseCategory().name())
                .dutyStatus(d.getDutyStatus().name())
                .safetyScore(d.getSafetyScore())
                .licenseNumber(d.getLicenseNumber())
                .latitude(loc.getLatitude())
                .longitude(loc.getLongitude())
                .accuracy(loc.getAccuracy())
                .speed(loc.getSpeed())
                .heading(loc.getHeading())
                .sharingActive(loc.getSharingActive())
                .lastUpdated(loc.getLastUpdated())
                .build();
    }

    /**
     * Convert LocationHistory entity to LocationHistoryResponse DTO.
     */
    private LocationHistoryResponse toHistoryResponse(LocationHistory history) {
        return LocationHistoryResponse.builder()
                .id(history.getId())
                .driverId(history.getDriverId())
                .latitude(history.getLatitude())
                .longitude(history.getLongitude())
                .accuracy(history.getAccuracy())
                .speed(history.getSpeed())
                .heading(history.getHeading())
                .recordedAt(history.getRecordedAt())
                .build();
    }
}
