package com.fleetflow.repository;

import com.fleetflow.entity.Trip;
import com.fleetflow.enums.TripStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

    Page<Trip> findByStatus(TripStatus status, Pageable pageable);

    List<Trip> findByVehicleId(Long vehicleId);

    List<Trip> findByDriverId(Long driverId);

    long countByStatus(TripStatus status);

    @Query("SELECT t FROM Trip t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:vehicleId IS NULL OR t.vehicle.id = :vehicleId) AND " +
           "(:driverId IS NULL OR t.driver.id = :driverId)")
    Page<Trip> findFiltered(@Param("status") TripStatus status,
                            @Param("vehicleId") Long vehicleId,
                            @Param("driverId") Long driverId,
                            Pageable pageable);

    @Query("SELECT COUNT(t) FROM Trip t WHERE t.driver.id = :driverId AND t.status = 'COMPLETED'")
    long countCompletedByDriver(@Param("driverId") Long driverId);

    @Query("SELECT COUNT(t) FROM Trip t WHERE t.driver.id = :driverId AND (t.status = 'COMPLETED' OR t.status = 'CANCELLED')")
    long countFinishedByDriver(@Param("driverId") Long driverId);
}
