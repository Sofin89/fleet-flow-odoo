package com.fleetflow.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for role-based access control.
 * Marks controller methods with allowed roles that can access the endpoint.
 * 
 * This annotation is processed by the RoleBasedAccessInterceptor to enforce
 * role-based access control at runtime.
 * 
 * Example usage:
 * <pre>
 * {@code
 * @RoleAllowed("DRIVER")
 * public ResponseEntity<?> startSharing() { ... }
 * 
 * @RoleAllowed({"MANAGER", "DISPATCHER"})
 * public ResponseEntity<?> getAllLocations() { ... }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RoleAllowed {
    /**
     * Array of role names that are allowed to access the annotated method.
     * 
     * @return array of allowed role names
     */
    String[] value();
}
