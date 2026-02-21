package com.fleetflow.repository;

import com.fleetflow.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AuditLog entity.
 * Provides custom query methods for finding audit logs by user, action type, and date range.
 * 
 * Requirements: 7.5
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find audit logs for a specific user within a date range.
     * Results are ordered by created_at in descending order (newest first).
     * Uses the idx_audit_logs_user_time index for efficient querying.
     * 
     * @param userId the user ID
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return list of audit log records
     */
    @Query("SELECT al FROM AuditLog al " +
           "WHERE al.userId = :userId " +
           "AND al.createdAt >= :startDate " +
           "AND al.createdAt <= :endDate " +
           "ORDER BY al.createdAt DESC")
    List<AuditLog> findByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find all audit logs for a specific user.
     * Results are ordered by created_at in descending order (newest first).
     * Uses the idx_audit_logs_user_time index for efficient querying.
     * 
     * @param userId the user ID
     * @return list of all audit log records for the user
     */
    @Query("SELECT al FROM AuditLog al " +
           "WHERE al.userId = :userId " +
           "ORDER BY al.createdAt DESC")
    List<AuditLog> findByUserId(@Param("userId") Long userId);

    /**
     * Find audit logs by action type within a date range.
     * Results are ordered by created_at in descending order (newest first).
     * Uses the idx_audit_logs_action_time index for efficient querying.
     * 
     * @param actionType the action type (e.g., "UNAUTHORIZED_ACCESS")
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return list of audit log records
     */
    @Query("SELECT al FROM AuditLog al " +
           "WHERE al.actionType = :actionType " +
           "AND al.createdAt >= :startDate " +
           "AND al.createdAt <= :endDate " +
           "ORDER BY al.createdAt DESC")
    List<AuditLog> findByActionTypeAndDateRange(
        @Param("actionType") String actionType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find all audit logs by action type.
     * Results are ordered by created_at in descending order (newest first).
     * Uses the idx_audit_logs_action_time index for efficient querying.
     * 
     * @param actionType the action type (e.g., "UNAUTHORIZED_ACCESS")
     * @return list of audit log records
     */
    @Query("SELECT al FROM AuditLog al " +
           "WHERE al.actionType = :actionType " +
           "ORDER BY al.createdAt DESC")
    List<AuditLog> findByActionType(@Param("actionType") String actionType);

    /**
     * Find audit logs by username within a date range.
     * Useful when user_id is null (user deleted) but username is preserved.
     * Uses the idx_audit_logs_username index for efficient querying.
     * 
     * @param username the username
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return list of audit log records
     */
    @Query("SELECT al FROM AuditLog al " +
           "WHERE al.username = :username " +
           "AND al.createdAt >= :startDate " +
           "AND al.createdAt <= :endDate " +
           "ORDER BY al.createdAt DESC")
    List<AuditLog> findByUsernameAndDateRange(
        @Param("username") String username,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find all audit logs by username.
     * Uses the idx_audit_logs_username index for efficient querying.
     * 
     * @param username the username
     * @return list of audit log records
     */
    @Query("SELECT al FROM AuditLog al " +
           "WHERE al.username = :username " +
           "ORDER BY al.createdAt DESC")
    List<AuditLog> findByUsername(@Param("username") String username);

    /**
     * Count audit logs for a specific user.
     * Useful for statistics and pagination.
     * 
     * @param userId the user ID
     * @return the count of audit log records
     */
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    /**
     * Count audit logs by action type.
     * Useful for security monitoring and statistics.
     * 
     * @param actionType the action type
     * @return the count of audit log records
     */
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.actionType = :actionType")
    long countByActionType(@Param("actionType") String actionType);

    /**
     * Find recent audit logs across all users.
     * Limited to the most recent entries for dashboard/monitoring purposes.
     * 
     * @param limit the maximum number of records to return
     * @return list of recent audit log records
     */
    @Query("SELECT al FROM AuditLog al " +
           "ORDER BY al.createdAt DESC " +
           "LIMIT :limit")
    List<AuditLog> findRecentLogs(@Param("limit") int limit);

    /**
     * Find unauthorized access attempts within a date range.
     * Specifically filters for UNAUTHORIZED_ACCESS action type.
     * 
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return list of unauthorized access audit logs
     */
    @Query("SELECT al FROM AuditLog al " +
           "WHERE al.actionType = 'UNAUTHORIZED_ACCESS' " +
           "AND al.createdAt >= :startDate " +
           "AND al.createdAt <= :endDate " +
           "ORDER BY al.createdAt DESC")
    List<AuditLog> findUnauthorizedAccessAttempts(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
