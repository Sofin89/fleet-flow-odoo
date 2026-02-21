package com.fleetflow.service;

import com.fleetflow.dto.MapMarkerDTO;
import com.fleetflow.entity.*;
import com.fleetflow.enums.DutyStatus;
import com.fleetflow.enums.TripStatus;
import com.fleetflow.enums.VehicleStatus;
import com.fleetflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MapTrackingService {

    private final DriverLocationRepository driverLocationRepo;
    private final VehicleLocationRepository vehicleLocationRepo;
    private final DriverRepository driverRepo;
    private final VehicleRepository vehicleRepo;
    private final TripRepository tripRepo;
    private final Random random = new Random();

    private static final double BASE_LAT = 23.0225;
    private static final double BASE_LNG = 72.5714;

    /**
     * Returns unified map markers: VEHICLE, DRIVER, or COMBINED (when a driver is on a dispatched trip).
     */
    public List<MapMarkerDTO> getAllMapMarkers() {
        List<DriverLocation> driverLocs = driverLocationRepo.findAll();
        List<VehicleLocation> vehicleLocs = vehicleLocationRepo.findAll();

        // Find active trips (DISPATCHED) — these pair a driver with a vehicle
        List<Trip> activeTrips = tripRepo.findByStatus(TripStatus.DISPATCHED);

        Set<Long> pairedDriverIds = new HashSet<>();
        Set<Long> pairedVehicleIds = new HashSet<>();
        List<MapMarkerDTO> markers = new ArrayList<>();

        // 1. COMBINED markers for active trips
        for (Trip trip : activeTrips) {
            Long driverId = trip.getDriver().getId();
            Long vehicleId = trip.getVehicle().getId();
            pairedDriverIds.add(driverId);
            pairedVehicleIds.add(vehicleId);

            DriverLocation dLoc = driverLocs.stream()
                    .filter(dl -> dl.getDriverId().equals(driverId)).findFirst().orElse(null);

            if (dLoc != null) {
                Driver driver = dLoc.getDriver();
                Vehicle vehicle = trip.getVehicle();
                markers.add(MapMarkerDTO.builder()
                        .markerType("COMBINED")
                        .driverId(driverId)
                        .vehicleId(vehicleId)
                        .vehicleName(vehicle.getName())
                        .vehicleModel(vehicle.getModel())
                        .licensePlate(vehicle.getLicensePlate())
                        .vehicleType(vehicle.getVehicleType().name())
                        .vehicleStatus(vehicle.getStatus().name())
                        .driverName(driver.getFullName())
                        .licenseCategory(driver.getLicenseCategory().name())
                        .dutyStatus(driver.getDutyStatus().name())
                        .safetyScore(driver.getSafetyScore())
                        .licenseNumber(driver.getLicenseNumber())
                        .latitude(dLoc.getLatitude())
                        .longitude(dLoc.getLongitude())
                        .speed(dLoc.getSpeed())
                        .heading(dLoc.getHeading())
                        .lastUpdated(dLoc.getLastUpdated())
                        .build());
            }
        }

        // 2. Standalone DRIVER markers (not on a trip)
        for (DriverLocation dLoc : driverLocs) {
            if (pairedDriverIds.contains(dLoc.getDriverId())) continue;
            Driver d = dLoc.getDriver();
            markers.add(MapMarkerDTO.builder()
                    .markerType("DRIVER")
                    .driverId(d.getId())
                    .driverName(d.getFullName())
                    .licenseCategory(d.getLicenseCategory().name())
                    .dutyStatus(d.getDutyStatus().name())
                    .safetyScore(d.getSafetyScore())
                    .licenseNumber(d.getLicenseNumber())
                    .latitude(dLoc.getLatitude())
                    .longitude(dLoc.getLongitude())
                    .speed(dLoc.getSpeed())
                    .heading(dLoc.getHeading())
                    .lastUpdated(dLoc.getLastUpdated())
                    .build());
        }

        // 3. Standalone VEHICLE markers (not on a trip)
        for (VehicleLocation vLoc : vehicleLocs) {
            if (pairedVehicleIds.contains(vLoc.getVehicleId())) continue;
            Vehicle v = vLoc.getVehicle();
            markers.add(MapMarkerDTO.builder()
                    .markerType("VEHICLE")
                    .vehicleId(v.getId())
                    .vehicleName(v.getName())
                    .vehicleModel(v.getModel())
                    .licensePlate(v.getLicensePlate())
                    .vehicleType(v.getVehicleType().name())
                    .vehicleStatus(v.getStatus().name())
                    .latitude(vLoc.getLatitude())
                    .longitude(vLoc.getLongitude())
                    .speed(vLoc.getSpeed())
                    .heading(vLoc.getHeading())
                    .lastUpdated(vLoc.getLastUpdated())
                    .build());
        }

        return markers;
    }

    @Transactional
    public void initializeVehicleLocations() {
        List<Vehicle> vehicles = vehicleRepo.findAll();
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle v = vehicles.get(i);
            if (!vehicleLocationRepo.existsById(v.getId())) {
                double[] offset = generateOffset(i + 10); // different offsets from drivers
                vehicleLocationRepo.save(VehicleLocation.builder()
                        .vehicle(v)
                        .latitude(BASE_LAT + offset[0])
                        .longitude(BASE_LNG + offset[1])
                        .speed(v.getStatus() == VehicleStatus.ON_TRIP
                                ? 20.0 + random.nextDouble() * 50.0 : 0.0)
                        .heading(random.nextDouble() * 360.0)
                        .lastUpdated(LocalDateTime.now())
                        .build());
            }
        }
        log.info("Initialized locations for {} vehicles", vehicles.size());
    }

    /**
     * Simulate vehicle movement every 5 seconds.
     * ON_TRIP vehicles move. AVAILABLE/IN_SHOP stay still.
     */
    @Scheduled(fixedRate = 5000)
    @Transactional
    public void simulateVehicleMovement() {
        List<VehicleLocation> locations = vehicleLocationRepo.findAll();
        for (VehicleLocation vLoc : locations) {
            Vehicle v = vLoc.getVehicle();
            if (v.getStatus() == VehicleStatus.ON_TRIP) {
                vLoc.setLatitude(vLoc.getLatitude() + (random.nextDouble() - 0.5) * 0.002);
                vLoc.setLongitude(vLoc.getLongitude() + (random.nextDouble() - 0.5) * 0.002);
                vLoc.setSpeed(Math.round((10 + random.nextDouble() * 60) * 10.0) / 10.0);
                vLoc.setHeading(random.nextDouble() * 360.0);
            } else {
                vLoc.setSpeed(0.0);
            }
            vLoc.setLastUpdated(LocalDateTime.now());
        }
        vehicleLocationRepo.saveAll(locations);
    }

    private double[] generateOffset(int index) {
        double[][] offsets = {
                {0.02, 0.01}, {-0.015, 0.025}, {0.035, -0.02},
                {-0.01, -0.03}, {0.005, 0.04}, {0.028, 0.015},
                {-0.025, -0.01}, {0.015, -0.035}, {-0.03, 0.02},
                {0.01, 0.03}, {-0.02, 0.015}, {0.03, -0.025},
                {-0.035, 0.01}, {0.025, 0.03}, {0.04, -0.01},
                {-0.008, 0.035}
        };
        double[] base = offsets[index % offsets.length];
        return new double[]{
                base[0] + (random.nextDouble() - 0.5) * 0.008,
                base[1] + (random.nextDouble() - 0.5) * 0.008
        };
    }
}
