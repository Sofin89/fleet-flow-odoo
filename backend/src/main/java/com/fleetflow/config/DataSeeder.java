package com.fleetflow.config;

import com.fleetflow.entity.*;
import com.fleetflow.enums.*;
import com.fleetflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        log.info("Seeding initial data...");

        // Create users
        userRepository.save(User.builder().email("manager@fleetflow.com").passwordHash(passwordEncoder.encode("password123")).fullName("John Manager").role(Role.MANAGER).active(true).build());
        userRepository.save(User.builder().email("dispatcher@fleetflow.com").passwordHash(passwordEncoder.encode("password123")).fullName("Sarah Dispatcher").role(Role.DISPATCHER).active(true).build());
        userRepository.save(User.builder().email("safety@fleetflow.com").passwordHash(passwordEncoder.encode("password123")).fullName("Mike Safety").role(Role.SAFETY_OFFICER).active(true).build());
        userRepository.save(User.builder().email("analyst@fleetflow.com").passwordHash(passwordEncoder.encode("password123")).fullName("Lisa Analyst").role(Role.ANALYST).active(true).build());

        // Create vehicles
        vehicleRepository.save(Vehicle.builder().name("Van-01").model("Ford Transit").licensePlate("FL-V001").vehicleType(VehicleType.VAN).maxLoadCapacityKg(new BigDecimal("800")).odometerKm(new BigDecimal("25000")).status(VehicleStatus.AVAILABLE).region("North").acquisitionCost(new BigDecimal("35000")).build());
        vehicleRepository.save(Vehicle.builder().name("Truck-01").model("Volvo FH16").licensePlate("FL-T001").vehicleType(VehicleType.TRUCK).maxLoadCapacityKg(new BigDecimal("5000")).odometerKm(new BigDecimal("80000")).status(VehicleStatus.AVAILABLE).region("South").acquisitionCost(new BigDecimal("120000")).build());
        vehicleRepository.save(Vehicle.builder().name("Van-02").model("Mercedes Sprinter").licensePlate("FL-V002").vehicleType(VehicleType.VAN).maxLoadCapacityKg(new BigDecimal("1200")).odometerKm(new BigDecimal("15000")).status(VehicleStatus.AVAILABLE).region("East").acquisitionCost(new BigDecimal("42000")).build());
        vehicleRepository.save(Vehicle.builder().name("Bike-01").model("Honda CB500X").licensePlate("FL-B001").vehicleType(VehicleType.BIKE).maxLoadCapacityKg(new BigDecimal("50")).odometerKm(new BigDecimal("5000")).status(VehicleStatus.AVAILABLE).region("Central").acquisitionCost(new BigDecimal("7000")).build());
        vehicleRepository.save(Vehicle.builder().name("Truck-02").model("Scania R500").licensePlate("FL-T002").vehicleType(VehicleType.TRUCK).maxLoadCapacityKg(new BigDecimal("8000")).odometerKm(new BigDecimal("120000")).status(VehicleStatus.AVAILABLE).region("West").acquisitionCost(new BigDecimal("150000")).build());
        vehicleRepository.save(Vehicle.builder().name("Van-05").model("Iveco Daily").licensePlate("FL-V005").vehicleType(VehicleType.VAN).maxLoadCapacityKg(new BigDecimal("500")).odometerKm(new BigDecimal("10000")).status(VehicleStatus.AVAILABLE).region("North").acquisitionCost(new BigDecimal("30000")).build());

        // Create drivers
        driverRepository.save(Driver.builder().fullName("Alex Thompson").licenseNumber("DL-001").licenseCategory(LicenseCategory.VAN).licenseExpiry(LocalDate.of(2027, 6, 15)).dutyStatus(DutyStatus.ON_DUTY).safetyScore(95).tripCompletionRate(98.5).phone("555-0101").email("alex@fleetflow.com").build());
        driverRepository.save(Driver.builder().fullName("Maria Garcia").licenseNumber("DL-002").licenseCategory(LicenseCategory.TRUCK).licenseExpiry(LocalDate.of(2027, 3, 20)).dutyStatus(DutyStatus.ON_DUTY).safetyScore(92).tripCompletionRate(96.0).phone("555-0102").email("maria@fleetflow.com").build());
        driverRepository.save(Driver.builder().fullName("James Wilson").licenseNumber("DL-003").licenseCategory(LicenseCategory.VAN).licenseExpiry(LocalDate.of(2026, 1, 10)).dutyStatus(DutyStatus.OFF_DUTY).safetyScore(88).tripCompletionRate(90.0).phone("555-0103").email("james@fleetflow.com").build());
        driverRepository.save(Driver.builder().fullName("Priya Patel").licenseNumber("DL-004").licenseCategory(LicenseCategory.BIKE).licenseExpiry(LocalDate.of(2028, 9, 1)).dutyStatus(DutyStatus.ON_DUTY).safetyScore(97).tripCompletionRate(100.0).phone("555-0104").email("priya@fleetflow.com").build());
        driverRepository.save(Driver.builder().fullName("Robert Chen").licenseNumber("DL-005").licenseCategory(LicenseCategory.TRUCK).licenseExpiry(LocalDate.of(2027, 12, 25)).dutyStatus(DutyStatus.ON_DUTY).safetyScore(90).tripCompletionRate(94.0).phone("555-0105").email("robert@fleetflow.com").build());

        log.info("Data seeded: 4 users, 6 vehicles, 5 drivers");
    }
}
