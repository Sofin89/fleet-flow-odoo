package com.fleetflow.dto;

import com.fleetflow.enums.Role;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private String token;
    private String email;
    private String fullName;
    private Role role;
    private Long userId;
}
