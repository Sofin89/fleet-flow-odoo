package com.fleetflow.service;

import com.fleetflow.dto.*;
import com.fleetflow.entity.Driver;
import com.fleetflow.entity.Trip;
import com.fleetflow.entity.Vehicle;
import com.fleetflow.enums.*;
import com.fleetflow.exception.BusinessException;
import com.fleetflow.exception.ResourceNotFoundException;
import com.fleetflow.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final VehicleService vehicleService;
    private final DriverService driverService;

    public Page<TripResponse> getAllTrips(TripStatus status, Long vehicleId, Long driverId, Pageable pageable) {
        return tripRepository.findFiltered(status, vehicleId, driverId, pageable)
                .map(this::toResponse);
    }

    public TripResponse getTrip(Long id) {
        return toResponse(findTripById(id));
    }

    @Transactional
    public TripResponse createTrip(TripRequest request) {
        Vehicle vehicle = vehicleService.findVehicleById(request.getVehicleId());
        Driver driver = driverService.findDriverById(request.getDriverId());

        // Business Rule: Check vehicle is available
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new BusinessException("Vehicle '" + vehicle.getName() + "' is not available. Current status: " + vehicle.getStatus());
        }

        // Business Rule: Check driver is on duty
        if (driver.getDutyStatus() != DutyStatus.ON_DUTY) {
            throw new BusinessException("Driver '" + driver.getFullName() + "' is not on duty. Current status: " + driver.getDutyStatus());
        }

        // Business Rule: Check license not expired
        if (!driver.isLicenseValid()) {
            throw new BusinessException("Driver '" + driver.getFullName() + "' has an expired license (Expired: " + driver.getLicenseExpiry() + ")");
        }

        // Business Rule: Check license category matches vehicle type
        if (!isLicenseCategoryCompatible(driver.getLicenseCategory(), vehicle.getVehicleType())) {
            throw new BusinessException("Driver's license category (" + driver.getLicenseCategory() +
                    ") is not valid for vehicle type (" + vehicle.getVehicleType() + ")");
        }

        // Business Rule: Cargo weight vs max capacity
        if (request.getCargoWeightKg().compareTo(vehicle.getMaxLoadCapacityKg()) > 0) {
            throw new BusinessException("Cargo weight (" + request.getCargoWeightKg() +
                    " kg) exceeds vehicle max capacity (" + vehicle.getMaxLoadCapacityKg() + " kg)");
        }

        Trip trip = Trip.builder()
                .vehicle(vehicle)
                .driver(driver)
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .cargoWeightKg(request.getCargoWeightKg())
                .revenue(request.getRevenue() != null ? request.getRevenue() : BigDecimal.ZERO)
                .startOdometer(vehicle.getOdometerKm())
                .status(TripStatus.DRAFT)
                .build();

        trip = tripRepository.save(trip);
        log.info("Trip created: {} -> {} (Vehicle: {}, Driver: {})",
                trip.getOrigin(), trip.getDestination(), vehicle.getLicensePlate(), driver.getFullName());
        return toResponse(trip);
    }

    @Transactional
    public TripResponse dispatchTrip(Long id) {
        Trip trip = findTripById(id);
        if (trip.getStatus() != TripStatus.DRAFT) {
            throw new BusinessException("Trip can only be dispatched from DRAFT status. Current: " + trip.getStatus());
        }

        trip.setStatus(TripStatus.DISPATCHED);

        // Update vehicle and driver status to ON_TRIP
        Vehicle vehicle = trip.getVehicle();
        vehicle.setStatus(VehicleStatus.ON_TRIP);

        tripRepository.save(trip);
        log.info("Trip {} dispatched", trip.getId());
        return toResponse(trip);
    }

    @Transactional
    public TripResponse completeTrip(Long id, TripCompleteRequest request) {
        Trip trip = findTripById(id);
        if (trip.getStatus() != TripStatus.DISPATCHED) {
            throw new BusinessException("Trip can only be completed from DISPATCHED status. Current: " + trip.getStatus());
        }

        trip.setStatus(TripStatus.COMPLETED);
        trip.setCompletedAt(LocalDateTime.now());

        if (request != null) {
            if (request.getEndOdometer() != null) {
                trip.setEndOdometer(request.getEndOdometer());
                // Update vehicle odometer
                trip.getVehicle().setOdometerKm(request.getEndOdometer());
            }
            if (request.getRevenue() != null) {
                trip.setRevenue(request.getRevenue());
            }
        }

        // Reset vehicle and driver status to AVAILABLE
        trip.getVehicle().setStatus(VehicleStatus.AVAILABLE);
        trip.getDriver().setDutyStatus(DutyStatus.ON_DUTY);

        tripRepository.save(trip);

        // Update driver stats
        driverService.updateDriverStats(trip.getDriver().getId());

        log.info("Trip {} completed", trip.getId());
        return toResponse(trip);
    }

    @Transactional
    public TripResponse cancelTrip(Long id) {
        Trip trip = findTripById(id);
        if (trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
            throw new BusinessException("Cannot cancel a trip that is already " + trip.getStatus());
        }

        trip.setStatus(TripStatus.CANCELLED);

        // If it was dispatched, reset vehicle and driver
        if (trip.getVehicle().getStatus() == VehicleStatus.ON_TRIP) {
            trip.getVehicle().setStatus(VehicleStatus.AVAILABLE);
        }
        if (trip.getDriver().getDutyStatus() != DutyStatus.SUSPENDED) {
            trip.getDriver().setDutyStatus(DutyStatus.ON_DUTY);
        }

        tripRepository.save(trip);

        // Update driver stats
        driverService.updateDriverStats(trip.getDriver().getId());

        log.info("Trip {} cancelled", trip.getId());
        return toResponse(trip);
    }

    private Trip findTripById(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with ID: " + id));
    }

    private boolean isLicenseCategoryCompatible(LicenseCategory license, VehicleType vehicleType) {
        return switch (vehicleType) {
            case TRUCK -> license == LicenseCategory.TRUCK;
            case VAN -> license == LicenseCategory.VAN || license == LicenseCategory.TRUCK;
            case BIKE -> license == LicenseCategory.BIKE;
        };
    }

    private TripResponse toResponse(Trip t) {
        return TripResponse.builder()
                .id(t.getId())
                .vehicleId(t.getVehicle().getId())
                .vehicleName(t.getVehicle().getName())
                .vehicleLicensePlate(t.getVehicle().getLicensePlate())
                .driverId(t.getDriver().getId())
                .driverName(t.getDriver().getFullName())
                .origin(t.getOrigin())
                .destination(t.getDestination())
                .cargoWeightKg(t.getCargoWeightKg())
                .maxCapacityKg(t.getVehicle().getMaxLoadCapacityKg())
                .revenue(t.getRevenue())
                .startOdometer(t.getStartOdometer())
                .endOdometer(t.getEndOdometer())
                .status(t.getStatus())
                .createdAt(t.getCreatedAt())
                .completedAt(t.getCompletedAt())
                .build();
    }
}
