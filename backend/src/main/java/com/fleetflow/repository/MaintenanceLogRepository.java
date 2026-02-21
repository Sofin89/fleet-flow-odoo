package com.fleetflow.repository;

import com.fleetflow.entity.MaintenanceLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface MaintenanceLogRepository extends JpaRepository<MaintenanceLog, Long> {

    List<MaintenanceLog> findByVehicleId(Long vehicleId);

    Page<MaintenanceLog> findByVehicleId(Long vehicleId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(m.cost), 0) FROM MaintenanceLog m WHERE m.vehicle.id = :vehicleId")
    BigDecimal getTotalMaintenanceCost(@Param("vehicleId") Long vehicleId);

    List<MaintenanceLog> findByCompletedFalse();

    @Query("SELECT m FROM MaintenanceLog m WHERE m.vehicle.id = :vehicleId AND m.serviceDate BETWEEN :start AND :end ORDER BY m.serviceDate DESC")
    List<MaintenanceLog> findByVehicleAndServiceDateBetween(
            @Param("vehicleId") Long vehicleId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
