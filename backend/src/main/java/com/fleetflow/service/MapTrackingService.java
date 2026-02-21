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

@Service
@RequiredArgsConstructor
@Slf4j
public class MapTrackingService {

    private final DriverLocationRepository driverLocationRepo;
    private final VehicleLocationRepository vehicleLocationRepo;
    private final DriverRepository driverRepo;
    private final VehicleRepository vehicleRepo;
    private final TripRepository tripRepo;
    private final OSRMRouteService osrmRouteService;
    private final Random random = new Random();

    private static final double BASE_LAT = 23.0225;
    private static final double BASE_LNG = 72.5714;

    // Known Ahmedabad/Gujarat location coordinates for trip routing
    private static final Map<String, double[]> LOCATION_COORDS = Map.ofEntries(
            Map.entry("Ahmedabad GIDC", new double[]{23.0734, 72.6327}),
            Map.entry("Surat Diamond Market", new double[]{21.1959, 72.8302}),
            Map.entry("Sanand Industrial Area", new double[]{22.9850, 72.3725}),
            Map.entry("Rajkot GIDC", new double[]{22.3039, 70.8022}),
            Map.entry("Naroda Industrial Estate", new double[]{23.0720, 72.6655}),
            Map.entry("Gandhinagar Infocity", new double[]{23.2156, 72.6369}),
            Map.entry("Vadodara GIDC", new double[]{22.3072, 73.1812}),
            Map.entry("Mundra Port", new double[]{22.8396, 69.7251}),
            Map.entry("Bhavnagar Port", new double[]{21.7645, 72.1519}),
            Map.entry("Mehsana GIDC", new double[]{23.5880, 72.3693})
    );

    /**
     * Returns unified map markers: VEHICLE, DRIVER, or COMBINED (with trip routes).
     */
    public List<MapMarkerDTO> getAllMapMarkers() {
        List<DriverLocation> driverLocs = driverLocationRepo.findAll();
        List<VehicleLocation> vehicleLocs = vehicleLocationRepo.findAll();
        List<Trip> activeTrips = tripRepo.findByStatus(TripStatus.DISPATCHED);

        Set<Long> pairedDriverIds = new HashSet<>();
        Set<Long> pairedVehicleIds = new HashSet<>();
        List<MapMarkerDTO> markers = new ArrayList<>();

        // 1. COMBINED markers for active trips (with route data)
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

                // Get origin/destination coordinates
                double[] originCoords = LOCATION_COORDS.getOrDefault(trip.getOrigin(), new double[]{BASE_LAT, BASE_LNG});
                double[] destCoords = LOCATION_COORDS.getOrDefault(trip.getDestination(), new double[]{BASE_LAT + 0.1, BASE_LNG + 0.1});

                // Fetch OSRM route with retry logic
                OSRMRouteService.RouteData route = osrmRouteService.getRoute(
                        originCoords[0], originCoords[1], destCoords[0], destCoords[1]
                );

                // Calculate progress using actual route distance
                double totalDist = route.getDistanceKm();
                double remainingDist = calculateRemainingDistance(
                        dLoc.getLatitude(), dLoc.getLongitude(),
                        route.getPolyline()
                );
                double progress = Math.min(100, Math.max(0,
                        ((totalDist - remainingDist) / totalDist) * 100));

                // Estimate time remaining (avg 45 km/h for heavy vehicles in Gujarat)
                double avgSpeed = dLoc.getSpeed() > 5 ? dLoc.getSpeed() : 45.0;
                int etaMinutes = (int) Math.ceil((remainingDist / avgSpeed) * 60);

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
                        // Trip route data
                        .tripOrigin(trip.getOrigin())
                        .tripDestination(trip.getDestination())
                        .originLat(originCoords[0])
                        .originLng(originCoords[1])
                        .destLat(destCoords[0])
                        .destLng(destCoords[1])
                        .totalDistanceKm(Math.round(totalDist * 10.0) / 10.0)
                        .remainingDistanceKm(Math.round(remainingDist * 10.0) / 10.0)
                        .progressPercent(Math.round(progress * 10.0) / 10.0)
                        .estimatedMinutesRemaining(etaMinutes)
                        // OSRM route data
                        .routePolyline(route.getPolyline())
                        .isRouteFallback(route.isFallback())
                        .build());
            }
        }

        // 2. Standalone DRIVER markers
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

        // 3. Standalone VEHICLE markers
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
        List<Trip> activeTrips = tripRepo.findByStatus(TripStatus.DISPATCHED);
        Map<Long, Trip> vehicleTripMap = new HashMap<>();
        for (Trip t : activeTrips) {
            vehicleTripMap.put(t.getVehicle().getId(), t);
        }

        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle v = vehicles.get(i);
            if (!vehicleLocationRepo.existsById(v.getId())) {
                double[] offset = generateOffset(i + 10);
                double lat = BASE_LAT + offset[0];
                double lng = BASE_LNG + offset[1];

                if (v.getStatus() == VehicleStatus.ON_TRIP) {
                    Trip t = vehicleTripMap.get(v.getId());
                    if (t != null) {
                        double[] originCoords = LOCATION_COORDS.getOrDefault(t.getOrigin(), new double[]{BASE_LAT, BASE_LNG});
                        lat = originCoords[0] + (random.nextDouble() - 0.5) * 0.002;
                        lng = originCoords[1] + (random.nextDouble() - 0.5) * 0.002;
                    }
                }

                vehicleLocationRepo.save(VehicleLocation.builder()
                        .vehicle(v)
                        .latitude(lat)
                        .longitude(lng)
                        .speed(v.getStatus() == VehicleStatus.ON_TRIP
                                ? 20.0 + random.nextDouble() * 50.0 : 0.0)
                        .heading(random.nextDouble() * 360.0)
                        .lastUpdated(LocalDateTime.now())
                        .build());
            }
        }
        log.info("Initialized locations for {} vehicles", vehicles.size());
    }

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void simulateVehicleMovement() {
        List<VehicleLocation> locations = vehicleLocationRepo.findAll();
        List<Trip> activeTrips = tripRepo.findByStatus(TripStatus.DISPATCHED);
        Map<Long, Trip> vehicleTripMap = new HashMap<>();
        for (Trip t : activeTrips) {
            vehicleTripMap.put(t.getVehicle().getId(), t);
        }

        for (VehicleLocation vLoc : locations) {
            Vehicle v = vLoc.getVehicle();
            if (v.getStatus() == VehicleStatus.ON_TRIP) {
                Trip t = vehicleTripMap.get(v.getId());
                if (t != null) {
                    double[] destCoords = LOCATION_COORDS.getOrDefault(t.getDestination(), new double[]{BASE_LAT, BASE_LNG});
                    double dLat = destCoords[0] - vLoc.getLatitude();
                    double dLon = destCoords[1] - vLoc.getLongitude();
                    double dist = Math.sqrt(dLat * dLat + dLon * dLon);

                    if (dist > 0.002) {
                        // Move ~0.002 degrees towards destination per tick
                        double step = 0.002 + (random.nextDouble() * 0.001);
                        vLoc.setLatitude(vLoc.getLatitude() + (dLat / dist) * step);
                        vLoc.setLongitude(vLoc.getLongitude() + (dLon / dist) * step);

                        // Set heading based on direction
                        double heading = Math.toDegrees(Math.atan2(dLon, dLat));
                        if (heading < 0) heading += 360;
                        vLoc.setHeading(heading);
                        vLoc.setSpeed(Math.round((30 + random.nextDouble() * 20) * 10.0) / 10.0);
                    } else {
                        vLoc.setSpeed(0.0);
                    }
                } else {
                    vLoc.setLatitude(vLoc.getLatitude() + (random.nextDouble() - 0.5) * 0.002);
                    vLoc.setLongitude(vLoc.getLongitude() + (random.nextDouble() - 0.5) * 0.002);
                    vLoc.setSpeed(Math.round((10 + random.nextDouble() * 60) * 10.0) / 10.0);
                    vLoc.setHeading(random.nextDouble() * 360.0);
                }
            } else {
                vLoc.setSpeed(0.0);
            }
            vLoc.setLastUpdated(LocalDateTime.now());
        }
        vehicleLocationRepo.saveAll(locations);
    }

    /** Haversine formula: returns distance in km between two lat/lng points. */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Calculate remaining distance along route polyline from current position.
     * Finds closest point on route and sums distances from there to destination.
     */
    private double calculateRemainingDistance(double currentLat, double currentLng,
                                              List<double[]> polyline) {
        if (polyline == null || polyline.isEmpty()) {
            return 0.0;
        }

        // Find closest point on route to current position
        int closestIdx = 0;
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < polyline.size(); i++) {
            double[] point = polyline.get(i);
            double dist = haversine(currentLat, currentLng, point[0], point[1]);
            if (dist < minDist) {
                minDist = dist;
                closestIdx = i;
            }
        }

        // Sum distances from closest point to destination
        double remaining = 0.0;
        for (int i = closestIdx; i < polyline.size() - 1; i++) {
            double[] p1 = polyline.get(i);
            double[] p2 = polyline.get(i + 1);
            remaining += haversine(p1[0], p1[1], p2[0], p2[1]);
        }

        return remaining;
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
