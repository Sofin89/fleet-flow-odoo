package com.fleetflow.service;

import com.fleetflow.entity.AuditLog;
import com.fleetflow.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditLogService.
 * Tests audit logging functionality for unauthorized access attempts.
 * 
 * Requirements: 7.5
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void logUnauthorizedAccess_shouldSaveAuditLogWithAllParameters() {
        // Given: Unauthorized access attempt parameters
        Long userId = 123L;
        String username = "testuser";
        String userRole = "DRIVER";
        String resourceUri = "/api/admin/users";
        String httpMethod = "GET";
        String ipAddress = "192.168.1.100";
        String userAgent = "Mozilla/5.0";

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: logUnauthorizedAccess is called
        auditLogService.logUnauthorizedAccess(
            userId, username, userRole, resourceUri, httpMethod, ipAddress, userAgent
        );

        // Then: AuditLog is saved with correct values
        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog savedLog = auditLogCaptor.getValue();

        assertEquals(userId, savedLog.getUserId());
        assertEquals(username, savedLog.getUsername());
        assertEquals(userRole, savedLog.getUserRole());
        assertEquals("UNAUTHORIZED_ACCESS", savedLog.getActionType());
        assertEquals(resourceUri, savedLog.getResourceUri());
        assertEquals(httpMethod, savedLog.getHttpMethod());
        assertEquals(ipAddress, savedLog.getIpAddress());
        assertEquals(userAgent, savedLog.getUserAgent());
    }

    @Test
    void logUnauthorizedAccess_shouldHandleNullUserId() {
        // Given: Unauthorized access attempt with null userId (deleted user)
        Long userId = null;
        String username = "deleteduser";
        String userRole = "DRIVER";
        String resourceUri = "/api/admin/users";
        String httpMethod = "POST";
        String ipAddress = "10.0.0.1";
        String userAgent = "Chrome/90.0";

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: logUnauthorizedAccess is called with null userId
        auditLogService.logUnauthorizedAccess(
            userId, username, userRole, resourceUri, httpMethod, ipAddress, userAgent
        );

        // Then: AuditLog is saved with null userId
        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog savedLog = auditLogCaptor.getValue();

        assertNull(savedLog.getUserId());
        assertEquals(username, savedLog.getUsername());
        assertEquals(userRole, savedLog.getUserRole());
        assertEquals("UNAUTHORIZED_ACCESS", savedLog.getActionType());
    }

    @Test
    void logUnauthorizedAccess_shouldNotThrowException_whenRepositorySaveFails() {
        // Given: Repository save operation fails
        when(auditLogRepository.save(any(AuditLog.class)))
            .thenThrow(new RuntimeException("Database connection failed"));

        // When: logUnauthorizedAccess is called
        // Then: No exception is thrown (method handles it gracefully)
        assertDoesNotThrow(() -> 
            auditLogService.logUnauthorizedAccess(
                1L, "testuser", "DRIVER", "/api/test", "GET", "127.0.0.1", "TestAgent"
            )
        );

        // And: Repository save was attempted
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void logUnauthorizedAccess_shouldSetActionTypeToUnauthorizedAccess() {
        // Given: Any unauthorized access attempt
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: logUnauthorizedAccess is called
        auditLogService.logUnauthorizedAccess(
            1L, "user", "ANALYST", "/api/trips", "DELETE", "192.168.1.1", "Safari"
        );

        // Then: actionType is always set to "UNAUTHORIZED_ACCESS"
        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog savedLog = auditLogCaptor.getValue();

        assertEquals("UNAUTHORIZED_ACCESS", savedLog.getActionType());
    }

    @Test
    void logUnauthorizedAccess_shouldHandleNullOptionalParameters() {
        // Given: Unauthorized access with null optional parameters
        Long userId = 1L;
        String username = "testuser";
        String userRole = "DRIVER";
        String resourceUri = "/api/test";
        String httpMethod = "GET";
        String ipAddress = null;
        String userAgent = null;

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: logUnauthorizedAccess is called with null optional parameters
        auditLogService.logUnauthorizedAccess(
            userId, username, userRole, resourceUri, httpMethod, ipAddress, userAgent
        );

        // Then: AuditLog is saved with null values for optional fields
        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog savedLog = auditLogCaptor.getValue();

        assertEquals(userId, savedLog.getUserId());
        assertEquals(username, savedLog.getUsername());
        assertEquals(userRole, savedLog.getUserRole());
        assertEquals(resourceUri, savedLog.getResourceUri());
        assertEquals(httpMethod, savedLog.getHttpMethod());
        assertNull(savedLog.getIpAddress());
        assertNull(savedLog.getUserAgent());
    }

    @Test
    void logUnauthorizedAccess_shouldSaveWithDifferentHttpMethods() {
        // Given: Different HTTP methods
        String[] httpMethods = {"GET", "POST", "PUT", "DELETE", "PATCH"};
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: logUnauthorizedAccess is called with different HTTP methods
        for (String method : httpMethods) {
            auditLogService.logUnauthorizedAccess(
                1L, "user", "DRIVER", "/api/test", method, "127.0.0.1", "Agent"
            );
        }

        // Then: All HTTP methods are saved correctly
        verify(auditLogRepository, times(httpMethods.length)).save(auditLogCaptor.capture());
        var savedLogs = auditLogCaptor.getAllValues();

        for (int i = 0; i < httpMethods.length; i++) {
            assertEquals(httpMethods[i], savedLogs.get(i).getHttpMethod());
        }
    }

    @Test
    void logUnauthorizedAccess_shouldSaveWithDifferentRoles() {
        // Given: Different user roles
        String[] roles = {"DRIVER", "ANALYST", "SAFETY_OFFICER", "DISPATCHER", "MANAGER"};
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: logUnauthorizedAccess is called with different roles
        for (String role : roles) {
            auditLogService.logUnauthorizedAccess(
                1L, "user", role, "/api/test", "GET", "127.0.0.1", "Agent"
            );
        }

        // Then: All roles are saved correctly
        verify(auditLogRepository, times(roles.length)).save(auditLogCaptor.capture());
        var savedLogs = auditLogCaptor.getAllValues();

        for (int i = 0; i < roles.length; i++) {
            assertEquals(roles[i], savedLogs.get(i).getUserRole());
        }
    }
}
