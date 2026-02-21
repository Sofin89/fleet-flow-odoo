package com.fleetflow.repository;

import com.fleetflow.entity.DriverLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverLocationRepository extends JpaRepository<DriverLocation, Long> {
    List<DriverLocation> findAll();
}
