package com.fleetflow.repository;

import com.fleetflow.entity.Driver;
import com.fleetflow.enums.DutyStatus;
import com.fleetflow.enums.LicenseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    List<Driver> findByDutyStatus(DutyStatus status);

    @Query("SELECT d FROM Driver d WHERE d.dutyStatus = 'ON_DUTY' AND d.licenseExpiry > :today AND d.licenseCategory = :category")
    List<Driver> findAvailableDrivers(@Param("today") LocalDate today, @Param("category") LicenseCategory category);

    @Query("SELECT d FROM Driver d WHERE " +
           "(:status IS NULL OR d.dutyStatus = :status) AND " +
           "(:category IS NULL OR d.licenseCategory = :category)")
    Page<Driver> findFiltered(@Param("status") DutyStatus status,
                              @Param("category") LicenseCategory category,
                              Pageable pageable);

    boolean existsByLicenseNumber(String licenseNumber);
}
