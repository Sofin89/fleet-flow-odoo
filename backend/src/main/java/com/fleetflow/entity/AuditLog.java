package com.fleetflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing audit log entries for security events.
 * Tracks unauthorized access attempts and other security-related actions.
 * 
 * Requirements: 7.5
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @NotBlank(message = "Username is required")
    @Size(max = 255, message = "Username must not exceed 255 characters")
    @Column(nullable = false, length = 255)
    private String username;

    @NotBlank(message = "User role is required")
    @Size(max = 50, message = "User role must not exceed 50 characters")
    @Column(name = "user_role", nullable = false, length = 50)
    private String userRole;

    @NotBlank(message = "Action type is required")
    @Size(max = 50, message = "Action type must not exceed 50 characters")
    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Size(max = 500, message = "Resource URI must not exceed 500 characters")
    @Column(name = "resource_uri", length = 500)
    private String resourceUri;

    @Size(max = 10, message = "HTTP method must not exceed 10 characters")
    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Size(max = 45, message = "IP address must not exceed 45 characters")
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @NotNull(message = "Created timestamp is required")
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
