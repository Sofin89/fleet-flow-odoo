package com.fleetflow.repository;

import com.fleetflow.entity.FuelLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface FuelLogRepository extends JpaRepository<FuelLog, Long> {

    List<FuelLog> findByVehicleId(Long vehicleId);

    Page<FuelLog> findByVehicleId(Long vehicleId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(f.cost), 0) FROM FuelLog f WHERE f.vehicle.id = :vehicleId")
    BigDecimal getTotalFuelCost(@Param("vehicleId") Long vehicleId);

    @Query("SELECT COALESCE(SUM(f.liters), 0) FROM FuelLog f WHERE f.vehicle.id = :vehicleId")
    BigDecimal getTotalLiters(@Param("vehicleId") Long vehicleId);
}
