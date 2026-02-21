package com.fleetflow.entity;

import com.fleetflow.enums.DutyStatus;
import com.fleetflow.enums.LicenseCategory;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "drivers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String licenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LicenseCategory licenseCategory;

    @Column(nullable = false)
    private LocalDate licenseExpiry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DutyStatus dutyStatus = DutyStatus.OFF_DUTY;

    @Column(nullable = false)
    private Integer safetyScore = 100;

    @Column(nullable = false)
    private Double tripCompletionRate = 0.0;

    @Column
    private String phone;

    @Column
    private String email;

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Trip> trips = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (dutyStatus == null) dutyStatus = DutyStatus.OFF_DUTY;
        if (safetyScore == null) safetyScore = 100;
        if (tripCompletionRate == null) tripCompletionRate = 0.0;
    }

    public boolean isLicenseValid() {
        return licenseExpiry != null && !licenseExpiry.isBefore(LocalDate.now());
    }
}
