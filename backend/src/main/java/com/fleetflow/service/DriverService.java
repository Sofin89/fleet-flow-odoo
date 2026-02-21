package com.fleetflow.service;

import com.fleetflow.dto.DriverRequest;
import com.fleetflow.dto.DriverResponse;
import com.fleetflow.entity.Driver;
import com.fleetflow.enums.DutyStatus;
import com.fleetflow.enums.LicenseCategory;
import com.fleetflow.exception.BusinessException;
import com.fleetflow.exception.ResourceNotFoundException;
import com.fleetflow.repository.DriverRepository;
import com.fleetflow.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverService {

    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;

    public Page<DriverResponse> getAllDrivers(DutyStatus status, LicenseCategory category, Pageable pageable) {
        return driverRepository.findFiltered(status, category, pageable)
                .map(this::toResponse);
    }

    public DriverResponse getDriver(Long id) {
        return toResponse(findDriverById(id));
    }

    @Transactional
    public DriverResponse createDriver(DriverRequest request) {
        if (driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new BusinessException("License number already exists: " + request.getLicenseNumber());
        }

        Driver driver = Driver.builder()
                .fullName(request.getFullName())
                .licenseNumber(request.getLicenseNumber())
                .licenseCategory(request.getLicenseCategory())
                .licenseExpiry(request.getLicenseExpiry())
                .dutyStatus(request.getDutyStatus() != null ? request.getDutyStatus() : DutyStatus.OFF_DUTY)
                .phone(request.getPhone())
                .email(request.getEmail())
                .safetyScore(100)
                .tripCompletionRate(0.0)
                .build();

        driver = driverRepository.save(driver);
        log.info("Driver created: {} (License: {})", driver.getFullName(), driver.getLicenseNumber());
        return toResponse(driver);
    }

    @Transactional
    public DriverResponse updateDriver(Long id, DriverRequest request) {
        Driver driver = findDriverById(id);
        driver.setFullName(request.getFullName());
        driver.setLicenseCategory(request.getLicenseCategory());
        driver.setLicenseExpiry(request.getLicenseExpiry());
        if (request.getPhone() != null) driver.setPhone(request.getPhone());
        if (request.getEmail() != null) driver.setEmail(request.getEmail());

        driver = driverRepository.save(driver);
        log.info("Driver updated: {}", driver.getFullName());
        return toResponse(driver);
    }

    @Transactional
    public DriverResponse updateStatus(Long id, DutyStatus status) {
        Driver driver = findDriverById(id);
        driver.setDutyStatus(status);
        driver = driverRepository.save(driver);
        log.info("Driver {} status changed to {}", driver.getFullName(), status);
        return toResponse(driver);
    }

    public List<DriverResponse> getAvailableDrivers(LicenseCategory category) {
        return driverRepository.findAvailableDrivers(LocalDate.now(), category).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    Driver findDriverById(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with ID: " + id));
    }

    void updateDriverStats(Long driverId) {
        long completed = tripRepository.countCompletedByDriver(driverId);
        long total = tripRepository.countFinishedByDriver(driverId);
        Driver driver = findDriverById(driverId);
        if (total > 0) {
            driver.setTripCompletionRate((double) completed / total * 100);
        }
        driverRepository.save(driver);
    }

    private DriverResponse toResponse(Driver d) {
        return DriverResponse.builder()
                .id(d.getId())
                .fullName(d.getFullName())
                .licenseNumber(d.getLicenseNumber())
                .licenseCategory(d.getLicenseCategory())
                .licenseExpiry(d.getLicenseExpiry())
                .dutyStatus(d.getDutyStatus())
                .safetyScore(d.getSafetyScore())
                .tripCompletionRate(d.getTripCompletionRate())
                .phone(d.getPhone())
                .email(d.getEmail())
                .licenseValid(d.isLicenseValid())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
