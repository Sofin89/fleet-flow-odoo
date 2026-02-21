package com.fleetflow.websocket;

import com.fleetflow.dto.DriverLocationResponse;
import com.fleetflow.dto.LocationUpdateEvent;
import com.fleetflow.dto.SharingStatusEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationWebSocketHandlerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private LocationWebSocketHandler handler;

    private DriverLocationResponse testLocation;

    @BeforeEach
    void setUp() {
        testLocation = DriverLocationResponse.builder()
                .driverId(1L)
                .fullName("Test Driver")
                .latitude(23.0225)
                .longitude(72.5714)
                .speed(45.0)
                .heading(180.0)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    @Test
    void broadcastLocationUpdate_shouldSendEventToTopicLocations() {
        // When
        handler.broadcastLocationUpdate(testLocation);

        // Then
        ArgumentCaptor<LocationUpdateEvent> eventCaptor = ArgumentCaptor.forClass(LocationUpdateEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/locations"), eventCaptor.capture());

        LocationUpdateEvent event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo("LOCATION_UPDATE");
        assertThat(event.getDriverId()).isEqualTo(1L);
        assertThat(event.getLatitude()).isEqualTo(23.0225);
        assertThat(event.getLongitude()).isEqualTo(72.5714);
        assertThat(event.getAccuracy()).isNull(); // Will be populated when accuracy field is added
        assertThat(event.getTimestamp()).isEqualTo(testLocation.getLastUpdated());
    }

    @Test
    void broadcastLocationUpdate_shouldNotThrowExceptionOnFailure() {
        // Given
        doThrow(new RuntimeException("WebSocket error"))
                .when(messagingTemplate).convertAndSend(anyString(), any(LocationUpdateEvent.class));

        // When/Then - should not throw exception
        handler.broadcastLocationUpdate(testLocation);

        // Verify the method was called despite the error
        verify(messagingTemplate).convertAndSend(eq("/topic/locations"), any(LocationUpdateEvent.class));
    }

    @Test
    void broadcastSharingStatusChange_shouldSendEventToTopicLocations() {
        // When
        handler.broadcastSharingStatusChange(1L, true);

        // Then
        ArgumentCaptor<SharingStatusEvent> eventCaptor = ArgumentCaptor.forClass(SharingStatusEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/locations"), eventCaptor.capture());

        SharingStatusEvent event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo("SHARING_STATUS_CHANGE");
        assertThat(event.getDriverId()).isEqualTo(1L);
        assertThat(event.getActive()).isTrue();
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    void broadcastSharingStatusChange_shouldSendInactiveStatus() {
        // When
        handler.broadcastSharingStatusChange(2L, false);

        // Then
        ArgumentCaptor<SharingStatusEvent> eventCaptor = ArgumentCaptor.forClass(SharingStatusEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/locations"), eventCaptor.capture());

        SharingStatusEvent event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo("SHARING_STATUS_CHANGE");
        assertThat(event.getDriverId()).isEqualTo(2L);
        assertThat(event.getActive()).isFalse();
    }

    @Test
    void broadcastSharingStatusChange_shouldNotThrowExceptionOnFailure() {
        // Given
        doThrow(new RuntimeException("WebSocket error"))
                .when(messagingTemplate).convertAndSend(anyString(), any(SharingStatusEvent.class));

        // When/Then - should not throw exception
        handler.broadcastSharingStatusChange(1L, true);

        // Verify the method was called despite the error
        verify(messagingTemplate).convertAndSend(eq("/topic/locations"), any(SharingStatusEvent.class));
    }
}
