package com.fleetflow.dto;

import com.fleetflow.enums.DutyStatus;
import com.fleetflow.enums.LicenseCategory;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DriverRequest {
    @NotBlank(message = "Driver full name is required")
    private String fullName;

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    @NotNull(message = "License category is required")
    private LicenseCategory licenseCategory;

    @NotNull(message = "License expiry date is required")
    private LocalDate licenseExpiry;

    private DutyStatus dutyStatus;
    private String phone;
    private String email;
}
