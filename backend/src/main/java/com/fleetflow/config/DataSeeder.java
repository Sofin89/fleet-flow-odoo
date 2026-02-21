package com.fleetflow.config;

import com.fleetflow.entity.*;
import com.fleetflow.enums.*;
import com.fleetflow.repository.*;
import com.fleetflow.service.DriverLocationService;
import com.fleetflow.service.MapTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;
    private final PasswordEncoder passwordEncoder;
    private final DriverLocationService driverLocationService;
    private final MapTrackingService mapTrackingService;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        log.info("Seeding initial data for FleetFlow India (Ahmedabad)...");

        // ─── Users ────────────────────────────────────────────────
        userRepository.save(User.builder().email("manager@fleetflow.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .fullName("Rajesh Sharma").role(Role.MANAGER).active(true).build());

        userRepository.save(User.builder().email("dispatcher@fleetflow.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .fullName("Priya Desai").role(Role.DISPATCHER).active(true).build());

        userRepository.save(User.builder().email("safety@fleetflow.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .fullName("Amit Patel").role(Role.SAFETY_OFFICER).active(true).build());

        userRepository.save(User.builder().email("analyst@fleetflow.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .fullName("Neha Joshi").role(Role.ANALYST).active(true).build());

        userRepository.save(User.builder().email("driver@fleetflow.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .fullName("Vikram Singh").role(Role.DRIVER).active(true).build());

        // ─── Vehicles (Indian fleet) ──────────────────────────────
        Vehicle tata01 = vehicleRepository.save(Vehicle.builder()
                .name("TATA-01").model("Tata Ace Gold").licensePlate("GJ-01-AB-1234")
                .vehicleType(VehicleType.VAN).maxLoadCapacityKg(new BigDecimal("750"))
                .odometerKm(new BigDecimal("42000")).status(VehicleStatus.AVAILABLE)
                .region("Ahmedabad").acquisitionCost(new BigDecimal("650000")).build());

        Vehicle ashok01 = vehicleRepository.save(Vehicle.builder()
                .name("AL-01").model("Ashok Leyland Dost Plus").licensePlate("GJ-01-CD-5678")
                .vehicleType(VehicleType.VAN).maxLoadCapacityKg(new BigDecimal("1500"))
                .odometerKm(new BigDecimal("28000")).status(VehicleStatus.AVAILABLE)
                .region("Gandhinagar").acquisitionCost(new BigDecimal("850000")).build());

        Vehicle truck01 = vehicleRepository.save(Vehicle.builder()
                .name("EICHER-01").model("Eicher Pro 3019").licensePlate("GJ-05-EF-9012")
                .vehicleType(VehicleType.TRUCK).maxLoadCapacityKg(new BigDecimal("9000"))
                .odometerKm(new BigDecimal("125000")).status(VehicleStatus.ON_TRIP)
                .region("Surat").acquisitionCost(new BigDecimal("2200000")).build());

        Vehicle truck02 = vehicleRepository.save(Vehicle.builder()
                .name("BHARAT-01").model("BharatBenz 1617R").licensePlate("GJ-06-GH-3456")
                .vehicleType(VehicleType.TRUCK).maxLoadCapacityKg(new BigDecimal("16000"))
                .odometerKm(new BigDecimal("180000")).status(VehicleStatus.ON_TRIP)
                .region("Rajkot").acquisitionCost(new BigDecimal("3500000")).build());

        Vehicle mahindra01 = vehicleRepository.save(Vehicle.builder()
                .name("MAHINDRA-01").model("Mahindra Bolero Pickup").licensePlate("GJ-01-IJ-7890")
                .vehicleType(VehicleType.VAN).maxLoadCapacityKg(new BigDecimal("1200"))
                .odometerKm(new BigDecimal("55000")).status(VehicleStatus.IN_SHOP)
                .region("Ahmedabad").acquisitionCost(new BigDecimal("900000")).build());

        Vehicle bajaj01 = vehicleRepository.save(Vehicle.builder()
                .name("BAJAJ-01").model("Bajaj Maxima C").licensePlate("GJ-01-KL-2345")
                .vehicleType(VehicleType.BIKE).maxLoadCapacityKg(new BigDecimal("500"))
                .odometerKm(new BigDecimal("15000")).status(VehicleStatus.AVAILABLE)
                .region("Ahmedabad").acquisitionCost(new BigDecimal("350000")).build());

        // ─── Drivers (Indian names) ──────────────────────────────
        Driver vikram = driverRepository.save(Driver.builder()
                .fullName("Vikram Singh").licenseNumber("GJ01-2021-001234")
                .licenseCategory(LicenseCategory.TRUCK).licenseExpiry(LocalDate.of(2028, 3, 15))
                .dutyStatus(DutyStatus.ON_DUTY).safetyScore(95).tripCompletionRate(98.5)
                .phone("+91 98765 43210").email("vikram@fleetflow.com").build());

        Driver anita = driverRepository.save(Driver.builder()
                .fullName("Anita Rathod").licenseNumber("GJ05-2020-005678")
                .licenseCategory(LicenseCategory.TRUCK).licenseExpiry(LocalDate.of(2027, 7, 20))
                .dutyStatus(DutyStatus.ON_DUTY).safetyScore(92).tripCompletionRate(96.0)
                .phone("+91 98765 43211").email("anita@fleetflow.com").build());

        Driver suresh = driverRepository.save(Driver.builder()
                .fullName("Suresh Parmar").licenseNumber("GJ01-2019-009012")
                .licenseCategory(LicenseCategory.VAN).licenseExpiry(LocalDate.of(2026, 11, 10))
                .dutyStatus(DutyStatus.OFF_DUTY).safetyScore(88).tripCompletionRate(90.0)
                .phone("+91 98765 43212").email("suresh@fleetflow.com").build());

        Driver kavita = driverRepository.save(Driver.builder()
                .fullName("Kavita Jadeja").licenseNumber("GJ06-2022-003456")
                .licenseCategory(LicenseCategory.VAN).licenseExpiry(LocalDate.of(2029, 1, 5))
                .dutyStatus(DutyStatus.ON_DUTY).safetyScore(97).tripCompletionRate(100.0)
                .phone("+91 98765 43213").email("kavita@fleetflow.com").build());

        Driver mohan = driverRepository.save(Driver.builder()
                .fullName("Mohan Thakor").licenseNumber("GJ01-2020-007890")
                .licenseCategory(LicenseCategory.BIKE).licenseExpiry(LocalDate.of(2027, 9, 30))
                .dutyStatus(DutyStatus.ON_DUTY).safetyScore(90).tripCompletionRate(94.0)
                .phone("+91 98765 43214").email("mohan@fleetflow.com").build());

        // ─── Active Trips (Gujarat routes) ────────────────────────
        tripRepository.save(Trip.builder()
                .vehicle(truck01).driver(anita)
                .origin("Ahmedabad GIDC").destination("Surat Diamond Market")
                .cargoWeightKg(new BigDecimal("7500"))
                .revenue(new BigDecimal("45000"))
                .status(TripStatus.DISPATCHED)
                .createdAt(LocalDateTime.now()).build());

        tripRepository.save(Trip.builder()
                .vehicle(truck02).driver(vikram)
                .origin("Sanand Industrial Area").destination("Rajkot GIDC")
                .cargoWeightKg(new BigDecimal("12000"))
                .revenue(new BigDecimal("72000"))
                .status(TripStatus.DISPATCHED)
                .createdAt(LocalDateTime.now()).build());

        // Completed trip for demo data
        tripRepository.save(Trip.builder()
                .vehicle(tata01).driver(kavita)
                .origin("Naroda Industrial Estate").destination("Gandhinagar Infocity")
                .cargoWeightKg(new BigDecimal("500"))
                .revenue(new BigDecimal("8500"))
                .status(TripStatus.COMPLETED)
                .createdAt(LocalDateTime.now().minusDays(1))
                .completedAt(LocalDateTime.now().minusHours(12)).build());

        // ─── Initialize GPS Locations ─────────────────────────────
        driverLocationService.initializeLocations();
        mapTrackingService.initializeVehicleLocations();

        log.info("FleetFlow India data seeded: 5 users, 6 vehicles, 5 drivers, 3 trips, GPS initialized (Ahmedabad)");
    }
}
