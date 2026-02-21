package com.fleetflow.repository;

import com.fleetflow.entity.Vehicle;
import com.fleetflow.enums.VehicleStatus;
import com.fleetflow.enums.VehicleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByStatus(VehicleStatus status);

    Page<Vehicle> findByStatusAndVehicleType(VehicleStatus status, VehicleType type, Pageable pageable);

    @Query("SELECT v FROM Vehicle v WHERE " +
           "(:status IS NULL OR v.status = :status) AND " +
           "(:type IS NULL OR v.vehicleType = :type) AND " +
           "(:region IS NULL OR v.region = :region)")
    Page<Vehicle> findFiltered(@Param("status") VehicleStatus status,
                               @Param("type") VehicleType type,
                               @Param("region") String region,
                               Pageable pageable);

    long countByStatus(VehicleStatus status);

    boolean existsByLicensePlate(String licensePlate);
}
