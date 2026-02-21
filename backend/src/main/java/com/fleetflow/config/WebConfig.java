package com.fleetflow.config;

import com.fleetflow.security.RoleBasedAccessInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for registering interceptors.
 * Registers the RoleBasedAccessInterceptor to enforce role-based access control.
 * 
 * Requirements: 7.1
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RoleBasedAccessInterceptor roleBasedAccessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(roleBasedAccessInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/auth/**"); // Exclude auth endpoints from RBAC
    }
}
