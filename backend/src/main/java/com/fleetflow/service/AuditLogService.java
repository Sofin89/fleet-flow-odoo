package com.fleetflow.service;

import com.fleetflow.entity.AuditLog;
import com.fleetflow.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing audit log entries.
 * Handles logging of security events, particularly unauthorized access attempts.
 * 
 * This service is called by the RoleBasedAccessInterceptor when access is denied
 * to log the attempt with user details, requested resource, and request metadata.
 * 
 * Requirements: 7.5
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Log an unauthorized access attempt.
     * Creates an audit log entry with action_type = "UNAUTHORIZED_ACCESS".
     * 
     * This method is called by RoleBasedAccessInterceptor when a user attempts
     * to access a resource they don't have permission for.
     * 
     * @param userId the user ID (may be null for deleted users)
     * @param username the username
     * @param userRole the user's role
     * @param resourceUri the requested resource URI
     * @param httpMethod the HTTP method (GET, POST, PUT, DELETE, etc.)
     * @param ipAddress the client IP address
     * @param userAgent the client user agent string
     */
    @Transactional
    public void logUnauthorizedAccess(
        Long userId,
        String username,
        String userRole,
        String resourceUri,
        String httpMethod,
        String ipAddress,
        String userAgent
    ) {
        try {
            AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .username(username)
                .userRole(userRole)
                .actionType("UNAUTHORIZED_ACCESS")
                .resourceUri(resourceUri)
                .httpMethod(httpMethod)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

            auditLogRepository.save(auditLog);
            
            log.info("Logged unauthorized access attempt: user={}, role={}, resource={}, method={}", 
                username, userRole, resourceUri, httpMethod);
        } catch (Exception e) {
            // Don't fail the request if audit logging fails
            log.error("Failed to save audit log for unauthorized access: user={}, resource={}", 
                username, resourceUri, e);
        }
    }
}
