package com.fleetflow.dto;

import com.fleetflow.enums.DutyStatus;
import com.fleetflow.enums.LicenseCategory;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DriverResponse {
    private Long id;
    private String fullName;
    private String licenseNumber;
    private LicenseCategory licenseCategory;
    private LocalDate licenseExpiry;
    private DutyStatus dutyStatus;
    private Integer safetyScore;
    private Double tripCompletionRate;
    private String phone;
    private String email;
    private Boolean licenseValid;
    private LocalDateTime createdAt;
}
