package com.yahya.uptime_monitor.service;

import com.sun.net.httpserver.HttpServer;
import com.yahya.uptime_monitor.config.MonitoringExecutionProperties;
import com.yahya.uptime_monitor.dto.WebsiteStats;
import com.yahya.uptime_monitor.model.MonitoringResult;
import com.yahya.uptime_monitor.model.Website;
import com.yahya.uptime_monitor.repository.IncidentRepository;
import com.yahya.uptime_monitor.repository.MonitoringResultRepository;
import com.yahya.uptime_monitor.repository.WebsiteRepository;
import com.yahya.uptime_monitor.repository.WebsiteStatsProjection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebsiteServiceBehaviorTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void createsAndNormalizesAValidWebsite() {
        Dependencies dependencies = dependencies();
        Website website = new Website(" Status API ", "HTTPS://Example.COM:443/");
        website.setTag(" production ");
        when(dependencies.websiteRepository.existsByUrl("https://example.com"))
                .thenReturn(false);
        when(dependencies.websiteRepository.save(website)).thenReturn(website);

        Website saved = dependencies.service.addWebsite(website);

        assertEquals("Status API", saved.getName());
        assertEquals("https://example.com", saved.getUrl());
        assertEquals("production", saved.getTag());
        verify(dependencies.websiteRepository).save(website);
    }

    @Test
    void calculatesUptimeStatisticsFromMonitoringHistory() {
        Dependencies dependencies = dependencies();
        LocalDateTime newest = LocalDateTime.of(2026, 8, 24, 12, 0);
        when(dependencies.websiteRepository.existsById(1L)).thenReturn(true);
        WebsiteStatsProjection projection = mock(WebsiteStatsProjection.class);
        when(projection.getTotalChecks()).thenReturn(4L);
        when(projection.getUpChecks()).thenReturn(3L);
        when(projection.getDownChecks()).thenReturn(1L);
        when(projection.getAverageResponseTime()).thenReturn(250.0);
        when(projection.getLastCheckedAt()).thenReturn(newest);
        when(dependencies.resultRepository.findStatsByWebsiteId(1L))
                .thenReturn(projection);

        WebsiteStats stats = dependencies.service.getStats(1L);

        assertEquals(4, stats.getTotalChecks());
        assertEquals(3, stats.getUpChecks());
        assertEquals(1, stats.getDownChecks());
        assertEquals(75.0, stats.getUptimePercentage());
        assertEquals(250, stats.getAverageResponseTime());
        assertEquals(newest, stats.getLastCheckedAt());
    }

    @Test
    void deletionRemovesRelatedDataBeforeTheWebsite() {
        Dependencies dependencies = dependencies();
        when(dependencies.websiteRepository.existsById(1L)).thenReturn(true);

        dependencies.service.deleteWebsite(1L);

        var ordered = inOrder(
                dependencies.resultRepository,
                dependencies.incidentRepository,
                dependencies.alertService,
                dependencies.websiteRepository
        );
        ordered.verify(dependencies.resultRepository).deleteByWebsiteId(1L);
        ordered.verify(dependencies.incidentRepository).deleteByWebsiteId(1L);
        ordered.verify(dependencies.alertService).deleteAlertsForWebsite(1L);
        ordered.verify(dependencies.websiteRepository).deleteById(1L);
    }

    @Test
    void recordsARealReadTimeoutAsADownMonitoringResult() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/slow", exchange -> {
            try {
                Thread.sleep(6_000);
                exchange.sendResponseHeaders(200, -1);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();

        Dependencies dependencies = dependencies();
        Website website = new Website(
                "Slow API",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/slow"
        );
        ReflectionTestUtils.setField(website, "id", 1L);
        website.setStatus("UP");
        website.setFailureThreshold(1);
        when(dependencies.websiteRepository.findById(1L)).thenReturn(Optional.of(website));
        when(dependencies.websiteRepository.save(website)).thenReturn(website);
        when(dependencies.incidentRepository
                .findFirstByWebsiteIdAndStatusOrderByStartedAtDesc(1L, "OPEN"))
                .thenReturn(Optional.empty());
        ArgumentCaptor<MonitoringResult> result = ArgumentCaptor.forClass(
                MonitoringResult.class
        );

        dependencies.service.checkWebsite(1L);

        verify(dependencies.resultRepository).save(result.capture());
        assertEquals("DOWN", website.getStatus());
        assertEquals("DOWN", result.getValue().getStatus());
        assertNull(result.getValue().getHttpStatusCode());
        assertNotNull(result.getValue().getFailureReason());
        assertTrue(result.getValue().getFailureReason().startsWith("TIMEOUT"));
        assertTrue(result.getValue().getResponseTime() >= 4_500);
    }

    private Dependencies dependencies() {
        WebsiteRepository websiteRepository = mock(WebsiteRepository.class);
        MonitoringResultRepository resultRepository = mock(MonitoringResultRepository.class);
        IncidentRepository incidentRepository = mock(IncidentRepository.class);
        AlertService alertService = mock(AlertService.class);
        MonitoringExecutionProperties properties = new MonitoringExecutionProperties();
        properties.setAllowPrivateTargets(true);
        WebsiteService service = new WebsiteService(
                websiteRepository,
                resultRepository,
                incidentRepository,
                alertService,
                properties,
                com.yahya.uptime_monitor.TestTransactionOperations.immediate()
        );
        return new Dependencies(
                service,
                websiteRepository,
                resultRepository,
                incidentRepository,
                alertService
        );
    }

    private record Dependencies(
            WebsiteService service,
            WebsiteRepository websiteRepository,
            MonitoringResultRepository resultRepository,
            IncidentRepository incidentRepository,
            AlertService alertService
    ) {
    }
}
