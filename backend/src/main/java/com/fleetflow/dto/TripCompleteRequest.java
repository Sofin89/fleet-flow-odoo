package com.fleetflow.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TripCompleteRequest {
    private BigDecimal endOdometer;
    private BigDecimal revenue;
}
