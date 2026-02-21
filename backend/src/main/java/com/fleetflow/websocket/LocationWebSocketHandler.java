package com.fleetflow.websocket;

import com.fleetflow.dto.DriverLocationResponse;
import com.fleetflow.dto.LocationUpdateEvent;
import com.fleetflow.dto.SharingStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class LocationWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast a location update to all connected clients viewing the LiveMap.
     * Failures are logged but do not throw exceptions to prevent failing the location update request.
     *
     * @param location The driver location response containing updated coordinates
     */
    public void broadcastLocationUpdate(DriverLocationResponse location) {
        try {
            LocationUpdateEvent event = LocationUpdateEvent.builder()
                    .type("LOCATION_UPDATE")
                    .driverId(location.getDriverId())
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude())
                    .accuracy(location.getAccuracy())
                    .timestamp(location.getLastUpdated())
                    .build();

            messagingTemplate.convertAndSend("/topic/locations", event);
            log.debug("Broadcast location update for driver {}", location.getDriverId());
        } catch (Exception e) {
            log.error("Failed to broadcast location update for driver {}: {}", 
                    location.getDriverId(), e.getMessage(), e);
            // Do not throw exception - WebSocket failures should not fail the location update request
        }
    }

    /**
     * Broadcast a sharing status change (start/stop) to all connected clients.
     * Failures are logged but do not throw exceptions to prevent failing the status change request.
     *
     * @param driverId The ID of the driver whose sharing status changed
     * @param active   True if sharing started, false if sharing stopped
     */
    public void broadcastSharingStatusChange(Long driverId, boolean active) {
        try {
            SharingStatusEvent event = SharingStatusEvent.builder()
                    .type("SHARING_STATUS_CHANGE")
                    .driverId(driverId)
                    .active(active)
                    .timestamp(LocalDateTime.now())
                    .build();

            messagingTemplate.convertAndSend("/topic/locations", event);
            log.debug("Broadcast sharing status change for driver {}: {}", driverId, active);
        } catch (Exception e) {
            log.error("Failed to broadcast sharing status change for driver {}: {}", 
                    driverId, e.getMessage(), e);
            // Do not throw exception - WebSocket failures should not fail the status change request
        }
    }
}
