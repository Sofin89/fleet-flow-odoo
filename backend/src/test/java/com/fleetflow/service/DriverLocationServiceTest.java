package com.fleetflow.service;

import com.fleetflow.dto.LocationHistoryResponse;
import com.fleetflow.entity.Driver;
import com.fleetflow.entity.LocationHistory;
import com.fleetflow.exception.BusinessException;
import com.fleetflow.exception.ResourceNotFoundException;
import com.fleetflow.repository.DriverLocationRepository;
import com.fleetflow.repository.DriverRepository;
import com.fleetflow.repository.LocationHistoryRepository;
import com.fleetflow.websocket.LocationWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DriverLocationService.
 * Tests location history query methods with date range validation.
 * 
 * Requirements: 11.4, 11.5
 */
@ExtendWith(MockitoExtension.class)
class DriverLocationServiceTest {

    @Mock
    private DriverLocationRepository locationRepo;

    @Mock
    private DriverRepository driverRepo;

    @Mock
    private LocationHistoryRepository historyRepo;

    @Mock
    private LocationWebSocketHandler wsHandler;

    @InjectMocks
    private DriverLocationService driverLocationService;

    @Test
    void getLocationHistory_shouldReturnHistoryWithinDateRange() {
        // Given: A driver with location history
        Long driverId = 1L;
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 15);
        
        Driver driver = new Driver();
        driver.setId(driverId);
        
        LocationHistory history1 = LocationHistory.builder()
                .id(1L)
                .driverId(driverId)
                .latitude(23.0225)
                .longitude(72.5714)
                .accuracy(10.0)
                .speed(50.0)
                .heading(180.0)
                .recordedAt(LocalDateTime.of(2024, 1, 5, 10, 30))
                .build();
        
        LocationHistory history2 = LocationHistory.builder()
                .id(2L)
                .driverId(driverId)
                .latitude(23.0300)
                .longitude(72.5800)
                .accuracy(15.0)
                .speed(60.0)
                .heading(90.0)
                .recordedAt(LocalDateTime.of(2024, 1, 10, 14, 45))
                .build();
        
        when(driverRepo.findById(driverId)).thenReturn(Optional.of(driver));
        when(historyRepo.findByDriverIdAndDateRange(
                eq(driverId),
                eq(startDate.atStartOfDay()),
                eq(endDate.atTime(23, 59, 59))
        )).thenReturn(Arrays.asList(history1, history2));

        // When: getLocationHistory is called
        List<LocationHistoryResponse> result = driverLocationService.getLocationHistory(driverId, startDate, endDate);

        // Then: History records are returned
        assertNotNull(result);
        assertEquals(2, result.size());
        
        LocationHistoryResponse response1 = result.get(0);
        assertEquals(1L, response1.getId());
        assertEquals(driverId, response1.getDriverId());
        assertEquals(23.0225, response1.getLatitude());
        assertEquals(72.5714, response1.getLongitude());
        assertEquals(10.0, response1.getAccuracy());
        assertEquals(50.0, response1.getSpeed());
        assertEquals(180.0, response1.getHeading());
        
        LocationHistoryResponse response2 = result.get(1);
        assertEquals(2L, response2.getId());
        assertEquals(driverId, response2.getDriverId());
        
        verify(driverRepo).findById(driverId);
        verify(historyRepo).findByDriverIdAndDateRange(any(), any(), any());
    }

    @Test
    void getLocationHistory_shouldThrowException_whenDriverNotFound() {
        // Given: Non-existent driver
        Long driverId = 999L;
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 15);
        
        when(driverRepo.findById(driverId)).thenReturn(Optional.empty());

        // When/Then: ResourceNotFoundException is thrown
        assertThrows(ResourceNotFoundException.class, () -> 
            driverLocationService.getLocationHistory(driverId, startDate, endDate)
        );
        
        verify(driverRepo).findById(driverId);
        verify(historyRepo, never()).findByDriverIdAndDateRange(any(), any(), any());
    }

    @Test
    void getLocationHistory_shouldThrowException_whenDateRangeExceeds30Days() {
        // Given: Date range exceeding 30 days
        Long driverId = 1L;
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 2, 5); // 35 days
        
        Driver driver = new Driver();
        driver.setId(driverId);
        
        when(driverRepo.findById(driverId)).thenReturn(Optional.of(driver));

        // When/Then: BusinessException is thrown
        BusinessException exception = assertThrows(BusinessException.class, () -> 
            driverLocationService.getLocationHistory(driverId, startDate, endDate)
        );
        
        assertTrue(exception.getMessage().contains("History range cannot exceed 30 days"));
        
        verify(driverRepo).findById(driverId);
        verify(historyRepo, never()).findByDriverIdAndDateRange(any(), any(), any());
    }

    @Test
    void getLocationHistory_shouldThrowException_whenStartDateAfterEndDate() {
        // Given: Invalid date range (start after end)
        Long driverId = 1L;
        LocalDate startDate = LocalDate.of(2024, 1, 15);
        LocalDate endDate = LocalDate.of(2024, 1, 1);
        
        Driver driver = new Driver();
        driver.setId(driverId);
        
        when(driverRepo.findById(driverId)).thenReturn(Optional.of(driver));

        // When/Then: BusinessException is thrown
        BusinessException exception = assertThrows(BusinessException.class, () -> 
            driverLocationService.getLocationHistory(driverId, startDate, endDate)
        );
        
        assertTrue(exception.getMessage().contains("Start date must be before or equal to end date"));
        
        verify(driverRepo).findById(driverId);
        verify(historyRepo, never()).findByDriverIdAndDateRange(any(), any(), any());
    }

    @Test
    void getLocationHistory_shouldAcceptExactly30DayRange() {
        // Given: Exactly 30-day range (boundary test)
        Long driverId = 1L;
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31); // Exactly 30 days
        
        Driver driver = new Driver();
        driver.setId(driverId);
        
        when(driverRepo.findById(driverId)).thenReturn(Optional.of(driver));
        when(historyRepo.findByDriverIdAndDateRange(any(), any(), any())).thenReturn(Arrays.asList());

        // When: getLocationHistory is called with 30-day range
        List<LocationHistoryResponse> result = driverLocationService.getLocationHistory(driverId, startDate, endDate);

        // Then: No exception is thrown and query is executed
        assertNotNull(result);
        verify(driverRepo).findById(driverId);
        verify(historyRepo).findByDriverIdAndDateRange(
                eq(driverId),
                eq(startDate.atStartOfDay()),
                eq(endDate.atTime(23, 59, 59))
        );
    }

    @Test
    void getLocationHistory_shouldReturnEmptyList_whenNoHistoryExists() {
        // Given: Driver with no location history
        Long driverId = 1L;
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 15);
        
        Driver driver = new Driver();
        driver.setId(driverId);
        
        when(driverRepo.findById(driverId)).thenReturn(Optional.of(driver));
        when(historyRepo.findByDriverIdAndDateRange(any(), any(), any())).thenReturn(Arrays.asList());

        // When: getLocationHistory is called
        List<LocationHistoryResponse> result = driverLocationService.getLocationHistory(driverId, startDate, endDate);

        // Then: Empty list is returned
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(driverRepo).findById(driverId);
        verify(historyRepo).findByDriverIdAndDateRange(any(), any(), any());
    }

    @Test
    void getLocationHistory_shouldConvertLocalDateToLocalDateTime() {
        // Given: LocalDate inputs
        Long driverId = 1L;
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 15);
        
        Driver driver = new Driver();
        driver.setId(driverId);
        
        when(driverRepo.findById(driverId)).thenReturn(Optional.of(driver));
        when(historyRepo.findByDriverIdAndDateRange(any(), any(), any())).thenReturn(Arrays.asList());

        // When: getLocationHistory is called
        driverLocationService.getLocationHistory(driverId, startDate, endDate);

        // Then: LocalDate is converted to LocalDateTime with correct time boundaries
        verify(historyRepo).findByDriverIdAndDateRange(
                eq(driverId),
                eq(LocalDateTime.of(2024, 1, 1, 0, 0, 0)),
                eq(LocalDateTime.of(2024, 1, 15, 23, 59, 59))
        );
    }

    @Test
    void getLocationHistory_shouldHandleSingleDayRange() {
        // Given: Single day range (start and end are the same)
        Long driverId = 1L;
        LocalDate startDate = LocalDate.of(2024, 1, 15);
        LocalDate endDate = LocalDate.of(2024, 1, 15);
        
        Driver driver = new Driver();
        driver.setId(driverId);
        
        LocationHistory history = LocationHistory.builder()
                .id(1L)
                .driverId(driverId)
                .latitude(23.0225)
                .longitude(72.5714)
                .recordedAt(LocalDateTime.of(2024, 1, 15, 12, 0))
                .build();
        
        when(driverRepo.findById(driverId)).thenReturn(Optional.of(driver));
        when(historyRepo.findByDriverIdAndDateRange(any(), any(), any())).thenReturn(Arrays.asList(history));

        // When: getLocationHistory is called with same start and end date
        List<LocationHistoryResponse> result = driverLocationService.getLocationHistory(driverId, startDate, endDate);

        // Then: History for that day is returned
        assertNotNull(result);
        assertEquals(1, result.size());
        
        verify(driverRepo).findById(driverId);
        verify(historyRepo).findByDriverIdAndDateRange(
                eq(driverId),
                eq(startDate.atStartOfDay()),
                eq(endDate.atTime(23, 59, 59))
        );
    }

    @Test
    void purgeOldLocationHistory_shouldDeleteRecordsOlderThan90Days() {
        // Given: Records older than 90 days exist
        int expectedDeletedCount = 150;
        
        when(historyRepo.deleteByRecordedAtBefore(any(LocalDateTime.class)))
                .thenReturn(expectedDeletedCount);

        // When: purgeOldLocationHistory is called
        driverLocationService.purgeOldLocationHistory();

        // Then: Records older than 90 days are deleted
        verify(historyRepo).deleteByRecordedAtBefore(any(LocalDateTime.class));
        
        // Verify the cutoff date is approximately 90 days ago (within 1 minute tolerance)
        verify(historyRepo).deleteByRecordedAtBefore(argThat(cutoffDate -> {
            LocalDateTime expectedCutoff = LocalDateTime.now().minusDays(90);
            long minutesDiff = Math.abs(java.time.Duration.between(cutoffDate, expectedCutoff).toMinutes());
            return minutesDiff <= 1; // Allow 1 minute tolerance for test execution time
        }));
    }

    @Test
    void purgeOldLocationHistory_shouldHandleExceptionGracefully() {
        // Given: Repository throws exception during delete
        when(historyRepo.deleteByRecordedAtBefore(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When: purgeOldLocationHistory is called
        // Then: No exception is thrown (exception is caught and logged)
        assertDoesNotThrow(() -> driverLocationService.purgeOldLocationHistory());
        
        verify(historyRepo).deleteByRecordedAtBefore(any(LocalDateTime.class));
    }

    @Test
    void purgeOldLocationHistory_shouldDeleteZeroRecords_whenNoOldRecordsExist() {
        // Given: No records older than 90 days
        when(historyRepo.deleteByRecordedAtBefore(any(LocalDateTime.class)))
                .thenReturn(0);

        // When: purgeOldLocationHistory is called
        driverLocationService.purgeOldLocationHistory();

        // Then: Delete operation is executed but returns 0
        verify(historyRepo).deleteByRecordedAtBefore(any(LocalDateTime.class));
    }
}
