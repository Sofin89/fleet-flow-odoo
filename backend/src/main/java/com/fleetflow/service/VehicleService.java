package com.fleetflow.service;

import com.fleetflow.dto.VehicleRequest;
import com.fleetflow.dto.VehicleResponse;
import com.fleetflow.entity.Vehicle;
import com.fleetflow.enums.VehicleStatus;
import com.fleetflow.enums.VehicleType;
import com.fleetflow.exception.BusinessException;
import com.fleetflow.exception.ResourceNotFoundException;
import com.fleetflow.repository.FuelLogRepository;
import com.fleetflow.repository.MaintenanceLogRepository;
import com.fleetflow.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final MaintenanceLogRepository maintenanceLogRepository;
    private final FuelLogRepository fuelLogRepository;

    public Page<VehicleResponse> getAllVehicles(VehicleStatus status, VehicleType type, String region, Pageable pageable) {
        return vehicleRepository.findFiltered(status, type, region, pageable)
                .map(this::toResponse);
    }

    public VehicleResponse getVehicle(Long id) {
        return toResponse(findVehicleById(id));
    }

    @Transactional
    public VehicleResponse createVehicle(VehicleRequest request) {
        if (vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new BusinessException("License plate already exists: " + request.getLicensePlate());
        }

        Vehicle vehicle = Vehicle.builder()
                .name(request.getName())
                .model(request.getModel())
                .licensePlate(request.getLicensePlate())
                .vehicleType(request.getVehicleType())
                .maxLoadCapacityKg(request.getMaxLoadCapacityKg())
                .odometerKm(request.getOdometerKm() != null ? request.getOdometerKm() : BigDecimal.ZERO)
                .status(request.getStatus() != null ? request.getStatus() : VehicleStatus.AVAILABLE)
                .region(request.getRegion())
                .acquisitionCost(request.getAcquisitionCost())
                .build();

        vehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle created: {} ({})", vehicle.getName(), vehicle.getLicensePlate());
        return toResponse(vehicle);
    }

    @Transactional
    public VehicleResponse updateVehicle(Long id, VehicleRequest request) {
        Vehicle vehicle = findVehicleById(id);
        vehicle.setName(request.getName());
        vehicle.setModel(request.getModel());
        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setMaxLoadCapacityKg(request.getMaxLoadCapacityKg());
        if (request.getOdometerKm() != null) vehicle.setOdometerKm(request.getOdometerKm());
        if (request.getRegion() != null) vehicle.setRegion(request.getRegion());
        if (request.getAcquisitionCost() != null) vehicle.setAcquisitionCost(request.getAcquisitionCost());

        vehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle updated: {} ({})", vehicle.getName(), vehicle.getLicensePlate());
        return toResponse(vehicle);
    }

    @Transactional
    public VehicleResponse updateStatus(Long id, VehicleStatus status) {
        Vehicle vehicle = findVehicleById(id);
        vehicle.setStatus(status);
        vehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle {} status changed to {}", vehicle.getLicensePlate(), status);
        return toResponse(vehicle);
    }

    public List<VehicleResponse> getAvailableVehicles() {
        return vehicleRepository.findByStatus(VehicleStatus.AVAILABLE).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    Vehicle findVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + id));
    }

    private VehicleResponse toResponse(Vehicle v) {
        BigDecimal maintenanceCost = maintenanceLogRepository.getTotalMaintenanceCost(v.getId());
        BigDecimal fuelCost = fuelLogRepository.getTotalFuelCost(v.getId());

        return VehicleResponse.builder()
                .id(v.getId())
                .name(v.getName())
                .model(v.getModel())
                .licensePlate(v.getLicensePlate())
                .vehicleType(v.getVehicleType())
                .maxLoadCapacityKg(v.getMaxLoadCapacityKg())
                .odometerKm(v.getOdometerKm())
                .status(v.getStatus())
                .region(v.getRegion())
                .acquisitionCost(v.getAcquisitionCost())
                .createdAt(v.getCreatedAt())
                .totalMaintenanceCost(maintenanceCost)
                .totalFuelCost(fuelCost)
                .build();
    }
}
