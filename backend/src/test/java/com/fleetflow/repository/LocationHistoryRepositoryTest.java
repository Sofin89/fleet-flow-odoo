package com.fleetflow.repository;

import com.fleetflow.entity.LocationHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LocationHistoryRepository.
 * Tests custom query methods for date range queries and purge operations.
 */
@DataJpaTest
@ActiveProfiles("test")
class LocationHistoryRepositoryTest {

    @Autowired
    private LocationHistoryRepository locationHistoryRepository;

    private Long testDriverId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        locationHistoryRepository.deleteAll();
        testDriverId = 1L;
        now = LocalDateTime.now();
    }

    @Test
    void testSaveLocationHistory() {
        // Given
        LocationHistory history = LocationHistory.builder()
                .driverId(testDriverId)
                .latitude(40.7128)
                .longitude(-74.0060)
                .accuracy(10.5)
                .speed(50.0)
                .heading(180.0)
                .recordedAt(now)
                .build();

        // When
        LocationHistory saved = locationHistoryRepository.save(history);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDriverId()).isEqualTo(testDriverId);
        assertThat(saved.getLatitude()).isEqualTo(40.7128);
        assertThat(saved.getLongitude()).isEqualTo(-74.0060);
    }

    @Test
    void testFindByDriverIdAndDateRange() {
        // Given
        LocalDateTime startDate = now.minusDays(5);
        LocalDateTime endDate = now;

        // Create records within range
        locationHistoryRepository.save(createHistory(testDriverId, now.minusDays(3)));
        locationHistoryRepository.save(createHistory(testDriverId, now.minusDays(2)));
        
        // Create record outside range
        locationHistoryRepository.save(createHistory(testDriverId, now.minusDays(10)));

        // When
        List<LocationHistory> results = locationHistoryRepository
                .findByDriverIdAndDateRange(testDriverId, startDate, endDate);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getRecordedAt()).isAfter(results.get(1).getRecordedAt());
    }

    @Test
    void testFindByDriverId() {
        // Given
        locationHistoryRepository.save(createHistory(testDriverId, now.minusDays(1)));
        locationHistoryRepository.save(createHistory(testDriverId, now.minusDays(2)));
        locationHistoryRepository.save(createHistory(2L, now.minusDays(1)));

        // When
        List<LocationHistory> results = locationHistoryRepository.findByDriverId(testDriverId);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(h -> h.getDriverId().equals(testDriverId));
    }

    @Test
    void testDeleteByRecordedAtBefore() {
        // Given
        LocalDateTime cutoffDate = now.minusDays(90);
        
        locationHistoryRepository.save(createHistory(testDriverId, now.minusDays(100)));
        locationHistoryRepository.save(createHistory(testDriverId, now.minusDays(95)));
        locationHistoryRepository.save(createHistory(testDriverId, now.minusDays(50)));

        // When
        int deletedCount = locationHistoryRepository.deleteByRecordedAtBefore(cutoffDate);

        // Then
        assertThat(deletedCount).isEqualTo(2);
        assertThat(locationHistoryRepository.findAll()).hasSize(1);
    }

    @Test
    void testCountByDriverId() {
        // Given
        locationHistoryRepository.save(createHistory(testDriverId, now));
        locationHistoryRepository.save(createHistory(testDriverId, now.minusDays(1)));
        locationHistoryRepository.save(createHistory(2L, now));

        // When
        long count = locationHistoryRepository.countByDriverId(testDriverId);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void testFindMostRecentByDriverId() {
        // Given
        LocalDateTime oldest = now.minusDays(3);
        LocalDateTime middle = now.minusDays(2);
        LocalDateTime newest = now.minusDays(1);

        locationHistoryRepository.save(createHistory(testDriverId, oldest));
        locationHistoryRepository.save(createHistory(testDriverId, middle));
        LocationHistory mostRecent = locationHistoryRepository.save(createHistory(testDriverId, newest));

        // When
        LocationHistory result = locationHistoryRepository.findMostRecentByDriverId(testDriverId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(mostRecent.getId());
        assertThat(result.getRecordedAt()).isEqualTo(newest);
    }

    @Test
    void testCountByRecordedAtBefore() {
        // Given
        LocalDateTime cutoffDate = now.minusDays(90);
        
        locationHistoryRepository.save(createHistory(testDriverId, now.minusDays(100)));
        locationHistoryRepository.save(createHistory(testDriverId, now.minusDays(95)));
        locationHistoryRepository.save(createHistory(testDriverId, now.minusDays(50)));

        // When
        long count = locationHistoryRepository.countByRecordedAtBefore(cutoffDate);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void testValidationConstraints() {
        // Given - location with valid coordinates
        LocationHistory history = LocationHistory.builder()
                .driverId(testDriverId)
                .latitude(40.7128)
                .longitude(-74.0060)
                .accuracy(10.5)
                .speed(50.0)
                .heading(180.0)
                .recordedAt(now)
                .build();

        // When
        LocationHistory saved = locationHistoryRepository.save(history);

        // Then
        assertThat(saved.getLatitude()).isBetween(-90.0, 90.0);
        assertThat(saved.getLongitude()).isBetween(-180.0, 180.0);
        assertThat(saved.getAccuracy()).isGreaterThanOrEqualTo(0.0);
        assertThat(saved.getSpeed()).isGreaterThanOrEqualTo(0.0);
        assertThat(saved.getHeading()).isBetween(0.0, 360.0);
    }

    private LocationHistory createHistory(Long driverId, LocalDateTime recordedAt) {
        return LocationHistory.builder()
                .driverId(driverId)
                .latitude(40.7128)
                .longitude(-74.0060)
                .accuracy(10.5)
                .speed(50.0)
                .heading(180.0)
                .recordedAt(recordedAt)
                .build();
    }
}
