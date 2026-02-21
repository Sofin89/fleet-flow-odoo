package com.fleetflow.security;

import com.fleetflow.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RoleBasedAccessInterceptor.
 * Tests role-based access control enforcement.
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5
 */
@ExtendWith(MockitoExtension.class)
class RoleBasedAccessInterceptorTest {

    private RoleBasedAccessInterceptor interceptor;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        interceptor = new RoleBasedAccessInterceptor(auditLogService);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void preHandle_shouldAllowAccess_whenNoRoleAllowedAnnotation() throws Exception {
        // Given: A handler method without @RoleAllowed annotation
        Object handler = new Object();

        // When: preHandle is called
        boolean result = interceptor.preHandle(request, response, handler);

        // Then: Access is allowed
        assertTrue(result);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void preHandle_shouldAllowAccess_whenUserHasRequiredRole() throws Exception {
        // Given: A user with DRIVER role
        Authentication auth = new UsernamePasswordAuthenticationToken(
            "testuser",
            "password",
            Collections.singletonList(new SimpleGrantedAuthority("DRIVER"))
        );
        when(securityContext.getAuthentication()).thenReturn(auth);

        // And: A handler method with @RoleAllowed("DRIVER")
        HandlerMethod handlerMethod = createHandlerMethod("driverOnlyMethod");

        // When: preHandle is called
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        // Then: Access is allowed
        assertTrue(result);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void preHandle_shouldDenyAccess_whenUserDoesNotHaveRequiredRole() throws Exception {
        // Given: A user with DRIVER role
        Authentication auth = new UsernamePasswordAuthenticationToken(
            "testuser",
            "password",
            Collections.singletonList(new SimpleGrantedAuthority("DRIVER"))
        );
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("User-Agent")).thenReturn("TestAgent");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // And: A handler method with @RoleAllowed({"MANAGER", "DISPATCHER"})
        HandlerMethod handlerMethod = createHandlerMethod("managerOnlyMethod");

        // When: preHandle is called
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        // Then: Access is denied with 403 Forbidden
        assertFalse(result);
        verify(response).sendError(
            eq(HttpServletResponse.SC_FORBIDDEN),
            eq("Access denied: insufficient permissions")
        );
        
        // And: AuditLogService is called to log the unauthorized access
        verify(auditLogService).logUnauthorizedAccess(
            isNull(), // userId is null when principal is not a User object
            eq("testuser"),
            eq("DRIVER"),
            eq("/api/test"),
            eq("GET"),
            eq("127.0.0.1"),
            eq("TestAgent")
        );
    }

    @Test
    void preHandle_shouldDenyAccess_whenUserIsNotAuthenticated() throws Exception {
        // Given: No authentication in SecurityContext
        when(securityContext.getAuthentication()).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/test");

        // And: A handler method with @RoleAllowed("DRIVER")
        HandlerMethod handlerMethod = createHandlerMethod("driverOnlyMethod");

        // When: preHandle is called
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        // Then: Access is denied with 401 Unauthorized
        assertFalse(result);
        verify(response).sendError(
            eq(HttpServletResponse.SC_UNAUTHORIZED),
            eq("Not authenticated")
        );
    }

    @Test
    void preHandle_shouldAllowAccess_whenUserHasOneOfMultipleAllowedRoles() throws Exception {
        // Given: A user with DISPATCHER role
        Authentication auth = new UsernamePasswordAuthenticationToken(
            "testuser",
            "password",
            Collections.singletonList(new SimpleGrantedAuthority("DISPATCHER"))
        );
        when(securityContext.getAuthentication()).thenReturn(auth);

        // And: A handler method with @RoleAllowed({"MANAGER", "DISPATCHER"})
        HandlerMethod handlerMethod = createHandlerMethod("managerOnlyMethod");

        // When: preHandle is called
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        // Then: Access is allowed
        assertTrue(result);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void preHandle_shouldDenyAccess_whenAuthenticationIsNotAuthenticated() throws Exception {
        // Given: An authentication that is not authenticated
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(request.getRequestURI()).thenReturn("/api/test");

        // And: A handler method with @RoleAllowed("DRIVER")
        HandlerMethod handlerMethod = createHandlerMethod("driverOnlyMethod");

        // When: preHandle is called
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        // Then: Access is denied with 401 Unauthorized
        assertFalse(result);
        verify(response).sendError(
            eq(HttpServletResponse.SC_UNAUTHORIZED),
            eq("Not authenticated")
        );
    }

    // Helper method to create HandlerMethod with @RoleAllowed annotation
    private HandlerMethod createHandlerMethod(String methodName) throws NoSuchMethodException {
        Method method = TestController.class.getMethod(methodName);
        TestController controller = new TestController();
        return new HandlerMethod(controller, method);
    }

    // Test controller with annotated methods
    static class TestController {
        @RoleAllowed("DRIVER")
        public void driverOnlyMethod() {
        }

        @RoleAllowed({"MANAGER", "DISPATCHER"})
        public void managerOnlyMethod() {
        }
    }
}
