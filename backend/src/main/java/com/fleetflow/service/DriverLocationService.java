package com.fleetflow.service;

import com.fleetflow.dto.DriverLocationResponse;
import com.fleetflow.entity.Driver;
import com.fleetflow.entity.DriverLocation;
import com.fleetflow.enums.DutyStatus;
import com.fleetflow.repository.DriverLocationRepository;
import com.fleetflow.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final Random random = new Random();

    // Ahmedabad-area base coordinates
    private static final double BASE_LAT = 23.0225;
    private static final double BASE_LNG = 72.5714;

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
                .speed(loc.getSpeed())
                .heading(loc.getHeading())
                .lastUpdated(loc.getLastUpdated())
                .build();
    }
}
