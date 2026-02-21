package com.fleetflow.service;

import com.fleetflow.dto.FuelLogRequest;
import com.fleetflow.dto.FuelLogResponse;
import com.fleetflow.entity.FuelLog;
import com.fleetflow.entity.Trip;
import com.fleetflow.entity.Vehicle;
import com.fleetflow.exception.ResourceNotFoundException;
import com.fleetflow.repository.FuelLogRepository;
import com.fleetflow.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FuelLogService {

    private final FuelLogRepository fuelLogRepository;
    private final VehicleService vehicleService;
    private final TripRepository tripRepository;

    public Page<FuelLogResponse> getAllLogs(Pageable pageable) {
        return fuelLogRepository.findAll(pageable).map(this::toResponse);
    }

    public Page<FuelLogResponse> getLogsByVehicle(Long vehicleId, Pageable pageable) {
        return fuelLogRepository.findByVehicleId(vehicleId, pageable).map(this::toResponse);
    }

    @Transactional
    public FuelLogResponse createLog(FuelLogRequest request) {
        Vehicle vehicle = vehicleService.findVehicleById(request.getVehicleId());

        Trip trip = null;
        if (request.getTripId() != null) {
            trip = tripRepository.findById(request.getTripId())
                    .orElseThrow(() -> new ResourceNotFoundException("Trip not found with ID: " + request.getTripId()));
        }

        FuelLog fuelLog = FuelLog.builder()
                .vehicle(vehicle)
                .trip(trip)
                .liters(request.getLiters())
                .cost(request.getCost())
                .logDate(request.getLogDate())
                .build();

        fuelLog = fuelLogRepository.save(fuelLog);
        log.info("Fuel log created for vehicle {} - {} liters", vehicle.getLicensePlate(), request.getLiters());
        return toResponse(fuelLog);
    }

    private FuelLogResponse toResponse(FuelLog f) {
        return FuelLogResponse.builder()
                .id(f.getId())
                .vehicleId(f.getVehicle().getId())
                .vehicleName(f.getVehicle().getName())
                .vehicleLicensePlate(f.getVehicle().getLicensePlate())
                .tripId(f.getTrip() != null ? f.getTrip().getId() : null)
                .liters(f.getLiters())
                .cost(f.getCost())
                .logDate(f.getLogDate())
                .createdAt(f.getCreatedAt())
                .build();
    }
}
