package com.yahya.uptime_monitor.service;

import com.yahya.uptime_monitor.config.MonitoringExecutionProperties;
import com.yahya.uptime_monitor.model.Website;
import com.yahya.uptime_monitor.repository.IncidentRepository;
import com.yahya.uptime_monitor.repository.MonitoringResultRepository;
import com.yahya.uptime_monitor.repository.WebsiteMetricsProjection;
import com.yahya.uptime_monitor.repository.WebsiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebsiteFilteringServiceTest {

    private WebsiteRepository websiteRepository;
    private MonitoringResultRepository resultRepository;
    private WebsiteService service;

    @BeforeEach
    void setUp() {
        websiteRepository = mock(WebsiteRepository.class);
        resultRepository = mock(MonitoringResultRepository.class);
        service = new WebsiteService(
                websiteRepository,
                resultRepository,
                mock(IncidentRepository.class),
                mock(AlertService.class),
                new MonitoringExecutionProperties(),
                com.yahya.uptime_monitor.TestTransactionOperations.immediate()
        );
    }

    @Test
    void filtersFieldsAndSortsByAggregatedUptimeDescending() {
        Website lowerUptime = website(1L, "API worker");
        Website higherUptime = website(2L, "API gateway");
        WebsiteMetricsProjection lowerMetrics = metrics(
                1L, 95.0, 500.0, LocalDateTime.of(2026, 8, 24, 10, 0)
        );
        WebsiteMetricsProjection higherMetrics = metrics(
                2L, 99.0, 100.0, LocalDateTime.of(2026, 8, 24, 11, 0)
        );
        when(websiteRepository.findFiltered("UP", true, "production", "api"))
                .thenReturn(List.of(lowerUptime, higherUptime));
        when(resultRepository.findMetricsByWebsiteIds(List.of(1L, 2L)))
                .thenReturn(List.of(lowerMetrics, higherMetrics));

        List<Website> result = service.findWebsites(
                "up",
                true,
                " production ",
                " api ",
                "uptimePercentage",
                "desc"
        );

        assertEquals(List.of(higherUptime, lowerUptime), result);
        verify(websiteRepository).findFiltered("UP", true, "production", "api");
    }

    @Test
    void paginatesTheFilteredAndSortedResult() {
        Website alpha = website(1L, "Alpha");
        Website beta = website(2L, "Beta");
        Website gamma = website(3L, "Gamma");
        when(websiteRepository.findFiltered(null, false, null, null))
                .thenReturn(List.of(gamma, alpha, beta));

        Page<Website> result = service.findWebsites(
                null,
                false,
                null,
                null,
                "name",
                "asc",
                PageRequest.of(1, 2)
        );

        assertEquals(List.of(gamma), result.getContent());
        assertEquals(3, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        verify(resultRepository, never()).findMetricsByWebsiteIds(anyList());
    }

    @Test
    void rejectsUnsupportedFilterAndSortValues() {
        ResponseStatusException status = assertThrows(
                ResponseStatusException.class,
                () -> service.findWebsites("BROKEN", null, null, null, null, null)
        );
        ResponseStatusException sort = assertThrows(
                ResponseStatusException.class,
                () -> service.findWebsites(null, null, null, null, "url", null)
        );
        ResponseStatusException direction = assertThrows(
                ResponseStatusException.class,
                () -> service.findWebsites(null, null, null, null, "name", "sideways")
        );

        assertEquals(400, status.getStatusCode().value());
        assertEquals(400, sort.getStatusCode().value());
        assertEquals(400, direction.getStatusCode().value());
    }

    private Website website(Long id, String name) {
        Website website = new Website(name, "https://example.com/" + id);
        ReflectionTestUtils.setField(website, "id", id);
        return website;
    }

    private WebsiteMetricsProjection metrics(
            Long websiteId,
            Double uptime,
            Double averageResponseTime,
            LocalDateTime lastCheckedAt
    ) {
        WebsiteMetricsProjection projection = mock(WebsiteMetricsProjection.class);
        when(projection.getWebsiteId()).thenReturn(websiteId);
        when(projection.getUptimePercentage()).thenReturn(uptime);
        when(projection.getAverageResponseTime()).thenReturn(averageResponseTime);
        when(projection.getLastCheckedAt()).thenReturn(lastCheckedAt);
        return projection;
    }
}
