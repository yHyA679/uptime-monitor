package com.yahya.uptime_monitor.service;

import com.yahya.uptime_monitor.config.MonitoringExecutionProperties;
import com.yahya.uptime_monitor.repository.MonitoringResultRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonitoringHistoryRetentionServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-24T12:00:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void deletesResultsOlderThanTheDefaultThirtyDayRetentionPeriod() {
        MonitoringResultRepository repository = mock(MonitoringResultRepository.class);
        MonitoringExecutionProperties properties = new MonitoringExecutionProperties();
        MonitoringHistoryRetentionService service = new MonitoringHistoryRetentionService(
                repository,
                properties,
                FIXED_CLOCK
        );
        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        when(repository.deleteExpiredBefore(cutoff.capture())).thenReturn(12);

        service.cleanupExpiredHistory();

        verify(repository).deleteExpiredBefore(cutoff.getValue());
        assertEquals(30, properties.getRetentionDays());
        assertEquals(LocalDateTime.of(2026, 7, 25, 12, 0), cutoff.getValue());
    }

    @Test
    void usesTheConfiguredRetentionPeriod() {
        MonitoringResultRepository repository = mock(MonitoringResultRepository.class);
        MonitoringExecutionProperties properties = new MonitoringExecutionProperties();
        properties.setRetentionDays(7);
        MonitoringHistoryRetentionService service = new MonitoringHistoryRetentionService(
                repository,
                properties,
                FIXED_CLOCK
        );
        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);

        service.cleanupExpiredHistory();

        verify(repository).deleteExpiredBefore(cutoff.capture());
        assertEquals(LocalDateTime.of(2026, 8, 17, 12, 0), cutoff.getValue());
    }
}
