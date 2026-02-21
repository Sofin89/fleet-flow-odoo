package com.fleetflow.service;

import com.fleetflow.dto.MaintenanceRequest;
import com.fleetflow.dto.MaintenanceResponse;
import com.fleetflow.entity.MaintenanceLog;
import com.fleetflow.entity.Vehicle;
import com.fleetflow.enums.VehicleStatus;
import com.fleetflow.exception.BusinessException;
import com.fleetflow.exception.ResourceNotFoundException;
import com.fleetflow.repository.MaintenanceLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceService {

    private final MaintenanceLogRepository maintenanceLogRepository;
    private final VehicleService vehicleService;

    public Page<MaintenanceResponse> getAllLogs(Pageable pageable) {
        return maintenanceLogRepository.findAll(pageable).map(this::toResponse);
    }

    public Page<MaintenanceResponse> getLogsByVehicle(Long vehicleId, Pageable pageable) {
        return maintenanceLogRepository.findByVehicleId(vehicleId, pageable).map(this::toResponse);
    }

    @Transactional
    public MaintenanceResponse createLog(MaintenanceRequest request) {
        Vehicle vehicle = vehicleService.findVehicleById(request.getVehicleId());

        // Auto-logic: Set vehicle status to IN_SHOP
        if (vehicle.getStatus() == VehicleStatus.ON_TRIP) {
            throw new BusinessException("Cannot add maintenance for a vehicle that is currently on a trip");
        }
        vehicle.setStatus(VehicleStatus.IN_SHOP);

        MaintenanceLog logEntry = MaintenanceLog.builder()
                .vehicle(vehicle)
                .serviceType(request.getServiceType())
                .description(request.getDescription())
                .cost(request.getCost())
                .serviceDate(request.getServiceDate())
                .completed(false)
                .build();

        logEntry = maintenanceLogRepository.save(logEntry);
        log.info("Maintenance log created for vehicle {} - {}", vehicle.getLicensePlate(), request.getServiceType());
        return toResponse(logEntry);
    }

    @Transactional
    public MaintenanceResponse completeMaintenance(Long id) {
        MaintenanceLog logEntry = maintenanceLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance log not found with ID: " + id));

        logEntry.setCompleted(true);

        // Set vehicle back to AVAILABLE
        Vehicle vehicle = logEntry.getVehicle();
        vehicle.setStatus(VehicleStatus.AVAILABLE);

        maintenanceLogRepository.save(logEntry);
        log.info("Maintenance completed for vehicle {}", vehicle.getLicensePlate());
        return toResponse(logEntry);
    }

    private MaintenanceResponse toResponse(MaintenanceLog m) {
        return MaintenanceResponse.builder()
                .id(m.getId())
                .vehicleId(m.getVehicle().getId())
                .vehicleName(m.getVehicle().getName())
                .vehicleLicensePlate(m.getVehicle().getLicensePlate())
                .serviceType(m.getServiceType())
                .description(m.getDescription())
                .cost(m.getCost())
                .serviceDate(m.getServiceDate())
                .completed(m.getCompleted())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
