package com.fleetflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharingStatusEvent {
    private String type;
    private Long driverId;
    private Boolean active;
    private LocalDateTime timestamp;
}
