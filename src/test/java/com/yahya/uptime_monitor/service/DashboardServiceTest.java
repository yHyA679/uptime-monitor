package com.yahya.uptime_monitor.service;

import com.yahya.uptime_monitor.dto.DashboardSummary;
import com.yahya.uptime_monitor.repository.DashboardMetricsProjection;
import com.yahya.uptime_monitor.repository.IncidentRepository;
import com.yahya.uptime_monitor.repository.MonitoringResultRepository;
import com.yahya.uptime_monitor.repository.WebsiteRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    @Test
    void buildsDashboardSummaryFromRepositoryAggregates() {
        WebsiteRepository websiteRepository = mock(WebsiteRepository.class);
        MonitoringResultRepository monitoringResultRepository =
                mock(MonitoringResultRepository.class);
        IncidentRepository incidentRepository = mock(IncidentRepository.class);
        DashboardMetricsProjection metrics = mock(DashboardMetricsProjection.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-24T12:00:00Z"),
                ZoneOffset.UTC
        );
        LocalDateTime cutoff = LocalDateTime.of(2026, 8, 23, 12, 0);

        when(websiteRepository.count()).thenReturn(12L);
        when(websiteRepository.countByEnabledTrueAndStatus("UP")).thenReturn(8L);
        when(websiteRepository.countByEnabledTrueAndStatus("DOWN")).thenReturn(2L);
        when(websiteRepository.countByEnabledFalse()).thenReturn(2L);
        when(metrics.getAverageUptime()).thenReturn(97.126);
        when(metrics.getAverageResponseTime()).thenReturn(184.6);
        when(monitoringResultRepository.findDashboardMetrics()).thenReturn(metrics);
        when(monitoringResultRepository.countByCheckedAtGreaterThanEqual(cutoff))
                .thenReturn(1_240L);
        when(incidentRepository.countByStatus("OPEN")).thenReturn(2L);
        when(incidentRepository.countByStartedAtGreaterThanEqual(cutoff)).thenReturn(5L);

        DashboardSummary summary = new DashboardService(
                websiteRepository,
                monitoringResultRepository,
                incidentRepository,
                clock
        ).getSummary();

        assertEquals(12, summary.totalWebsites());
        assertEquals(8, summary.upWebsites());
        assertEquals(2, summary.downWebsites());
        assertEquals(2, summary.disabledWebsites());
        assertEquals(97.13, summary.averageUptime());
        assertEquals(185, summary.averageResponseTime());
        assertEquals(2, summary.openIncidents());
        assertEquals(5, summary.recentIncidentCount());
        assertEquals(1_240, summary.checksLast24Hours());
        verify(monitoringResultRepository).countByCheckedAtGreaterThanEqual(cutoff);
        verify(incidentRepository).countByStartedAtGreaterThanEqual(cutoff);
    }

    @Test
    void returnsZeroAveragesWhenNoMonitoringHistoryExists() {
        WebsiteRepository websiteRepository = mock(WebsiteRepository.class);
        MonitoringResultRepository monitoringResultRepository =
                mock(MonitoringResultRepository.class);
        IncidentRepository incidentRepository = mock(IncidentRepository.class);

        DashboardSummary summary = new DashboardService(
                websiteRepository,
                monitoringResultRepository,
                incidentRepository,
                Clock.systemUTC()
        ).getSummary();

        assertEquals(0.0, summary.averageUptime());
        assertEquals(0, summary.averageResponseTime());
    }
}
