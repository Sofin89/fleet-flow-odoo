package com.fleetflow.security;

import com.fleetflow.entity.User;
import com.fleetflow.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * Interceptor for role-based access control.
 * Checks if the authenticated user's role is allowed to access the requested endpoint
 * based on the @RoleAllowed annotation on controller methods.
 * 
 * This interceptor:
 * - Extracts the user's role from Spring Security's SecurityContext
 * - Checks if the method has @RoleAllowed annotation
 * - Verifies the user's role is in the allowed roles list
 * - Returns 403 Forbidden if unauthorized
 * - Logs unauthorized access attempts via AuditLogService
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoleBasedAccessInterceptor implements HandlerInterceptor {

    private final AuditLogService auditLogService;

    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) throws Exception {
        
        // Only process HandlerMethod (controller methods)
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RoleAllowed roleAllowed = handlerMethod.getMethodAnnotation(RoleAllowed.class);
        
        // If no @RoleAllowed annotation, allow access (no role restriction)
        if (roleAllowed == null) {
            return true;
        }
        
        // Get authentication from SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // If not authenticated, return 401 Unauthorized
        if (auth == null || !auth.isAuthenticated()) {
            log.warn("Unauthenticated access attempt to protected endpoint: {}", 
                request.getRequestURI());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not authenticated");
            return false;
        }
        
        // Extract user role from authorities
        String userRole = auth.getAuthorities().stream()
            .findFirst()
            .map(GrantedAuthority::getAuthority)
            .orElse("");
        
        // Get allowed roles from annotation
        String[] allowedRoles = roleAllowed.value();
        
        // Check if user's role is in the allowed roles list
        boolean hasAccess = Arrays.asList(allowedRoles).contains(userRole);
        
        if (!hasAccess) {
            // Extract user details for audit logging
            Long userId = null;
            if (auth.getPrincipal() instanceof User) {
                userId = ((User) auth.getPrincipal()).getId();
            }
            
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            // Log unauthorized access attempt via AuditLogService
            auditLogService.logUnauthorizedAccess(
                userId,
                auth.getName(),
                userRole,
                request.getRequestURI(),
                request.getMethod(),
                ipAddress,
                userAgent
            );
            
            log.warn("Unauthorized access attempt: user={}, role={}, endpoint={}, method={}", 
                auth.getName(), 
                userRole, 
                request.getRequestURI(),
                request.getMethod());
            
            // Return 403 Forbidden
            response.sendError(HttpServletResponse.SC_FORBIDDEN, 
                "Access denied: insufficient permissions");
            return false;
        }
        
        // User has required role, allow access
        return true;
    }

    /**
     * Extract the client IP address from the request.
     * Checks common proxy headers (X-Forwarded-For, X-Real-IP) before falling back to remote address.
     * 
     * @param request the HTTP request
     * @return the client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        // X-Forwarded-For can contain multiple IPs, take the first one
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress;
    }
}
