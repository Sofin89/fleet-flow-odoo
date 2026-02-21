package com.fleetflow.repository;

import com.fleetflow.entity.VehicleLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleLocationRepository extends JpaRepository<VehicleLocation, Long> {
}
