package com.fleetflow.repository;

import com.fleetflow.entity.LocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for LocationHistory entity.
 * Provides custom query methods for date range queries and purge operations.
 * 
 * Requirements: 11.3, 11.4, 11.5
 */
@Repository
public interface LocationHistoryRepository extends JpaRepository<LocationHistory, Long> {

    /**
     * Find location history for a specific driver within a date range.
     * Results are ordered by recorded_at in descending order (newest first).
     * Uses the idx_location_history_driver_time index for efficient querying.
     * 
     * @param driverId the driver ID
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return list of location history records
     */
    @Query("SELECT lh FROM LocationHistory lh " +
           "WHERE lh.driverId = :driverId " +
           "AND lh.recordedAt >= :startDate " +
           "AND lh.recordedAt <= :endDate " +
           "ORDER BY lh.recordedAt DESC")
    List<LocationHistory> findByDriverIdAndDateRange(
        @Param("driverId") Long driverId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find all location history for a specific driver.
     * Results are ordered by recorded_at in descending order (newest first).
     * 
     * @param driverId the driver ID
     * @return list of all location history records for the driver
     */
    @Query("SELECT lh FROM LocationHistory lh " +
           "WHERE lh.driverId = :driverId " +
           "ORDER BY lh.recordedAt DESC")
    List<LocationHistory> findByDriverId(@Param("driverId") Long driverId);

    /**
     * Delete location history records older than the specified date.
     * This method is used by the scheduled purge job to maintain 90-day retention.
     * Uses the idx_location_history_recorded_at index for efficient deletion.
     * 
     * @param cutoffDate the date before which records should be deleted
     * @return the number of records deleted
     */
    @Modifying
    @Query("DELETE FROM LocationHistory lh WHERE lh.recordedAt < :cutoffDate")
    int deleteByRecordedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Count location history records for a specific driver.
     * Useful for statistics and pagination.
     * 
     * @param driverId the driver ID
     * @return the count of location history records
     */
    @Query("SELECT COUNT(lh) FROM LocationHistory lh WHERE lh.driverId = :driverId")
    long countByDriverId(@Param("driverId") Long driverId);

    /**
     * Find the most recent location history record for a specific driver.
     * 
     * @param driverId the driver ID
     * @return the most recent location history record, or null if none exists
     */
    @Query("SELECT lh FROM LocationHistory lh " +
           "WHERE lh.driverId = :driverId " +
           "ORDER BY lh.recordedAt DESC " +
           "LIMIT 1")
    LocationHistory findMostRecentByDriverId(@Param("driverId") Long driverId);

    /**
     * Count records older than the specified date.
     * Useful for monitoring before running purge operations.
     * 
     * @param cutoffDate the date to check against
     * @return the count of records older than the cutoff date
     */
    @Query("SELECT COUNT(lh) FROM LocationHistory lh WHERE lh.recordedAt < :cutoffDate")
    long countByRecordedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}
