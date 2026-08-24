package com.yahya.uptime_monitor.service;

import com.sun.net.httpserver.HttpServer;
import com.yahya.uptime_monitor.config.MonitoringExecutionProperties;
import com.yahya.uptime_monitor.model.Incident;
import com.yahya.uptime_monitor.model.MonitoringResult;
import com.yahya.uptime_monitor.model.Website;
import com.yahya.uptime_monitor.dto.WebsiteSettingsRequest;
import com.yahya.uptime_monitor.repository.IncidentRepository;
import com.yahya.uptime_monitor.repository.MonitoringResultRepository;
import com.yahya.uptime_monitor.repository.WebsiteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebsiteServiceTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void appliesFailureAndRecoveryThresholdsWithoutLosingRawResults() throws Exception {
        AtomicInteger responseCode = new AtomicInteger(503);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/health", exchange -> {
            byte[] body = "health".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseCode.get(), body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

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

        Website website = new Website(
                "Local health check",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/health"
        );
        ReflectionTestUtils.setField(website, "id", 1L);
        website.setStatus("UP");
        website.setFailureThreshold(3);
        website.setRecoveryThreshold(2);

        List<MonitoringResult> savedResults = new ArrayList<>();
        AtomicReference<Incident> openIncident = new AtomicReference<>();

        when(websiteRepository.findById(1L)).thenReturn(Optional.of(website));
        when(websiteRepository.save(any(Website.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(resultRepository.save(any(MonitoringResult.class))).thenAnswer(invocation -> {
            MonitoringResult result = invocation.getArgument(0);
            savedResults.add(result);
            return result;
        });
        when(incidentRepository.findFirstByWebsiteIdAndStatusOrderByStartedAtDesc(1L, "OPEN"))
                .thenAnswer(invocation -> Optional.ofNullable(openIncident.get()));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> {
            Incident incident = invocation.getArgument(0);
            if ("OPEN".equals(incident.getStatus())) {
                openIncident.set(incident);
            }
            return incident;
        });

        service.checkWebsite(1L);
        service.checkWebsite(1L);

        assertEquals("UP", website.getStatus());
        assertEquals(2, website.getConsecutiveFailures());

        service.checkWebsite(1L);

        assertEquals("DOWN", website.getStatus());
        assertEquals(3, website.getConsecutiveFailures());
        assertNotNull(website.getLastFailedCheckAt());
        assertEquals(503, website.getLastHttpStatusCode());
        assertTrue(website.getLastFailureReason().startsWith("HTTP 503"));
        verify(incidentRepository, times(1)).save(any(Incident.class));
        verify(alertService, times(1)).recordDowntime(
                any(Website.class),
                any(LocalDateTime.class)
        );

        responseCode.set(200);
        service.checkWebsite(1L);

        assertEquals("DOWN", website.getStatus());
        assertEquals(1, website.getConsecutiveSuccesses());

        service.checkWebsite(1L);

        assertEquals("UP", website.getStatus());
        assertEquals(0, website.getConsecutiveFailures());
        assertEquals(2, website.getConsecutiveSuccesses());
        assertNotNull(website.getLastSuccessfulCheckAt());
        assertEquals(200, website.getLastHttpStatusCode());
        assertNotNull(openIncident.get().getResolvedAt());
        assertEquals("RESOLVED", openIncident.get().getStatus());
        verify(alertService, times(1)).recordRecovery(
                any(Website.class),
                any(LocalDateTime.class)
        );

        assertEquals(5, savedResults.size());
        assertEquals(List.of("DOWN", "DOWN", "DOWN", "UP", "UP"),
                savedResults.stream().map(MonitoringResult::getStatus).toList());
        assertEquals(503, savedResults.get(0).getHttpStatusCode());
        assertTrue(savedResults.get(0).getFailureReason().startsWith("HTTP 503"));
        assertNull(savedResults.get(4).getFailureReason());
    }

    @Test
    void classifiesCommonNetworkFailures() {
        assertTrue(WebsiteService.classifyFailure(
                new UnknownHostException("missing.test")
        ).startsWith("DNS_RESOLUTION_FAILED"));
        assertTrue(WebsiteService.classifyFailure(
                new SocketTimeoutException("Read timed out")
        ).startsWith("TIMEOUT"));
        assertTrue(WebsiteService.classifyFailure(
                new ConnectException("Connection refused")
        ).startsWith("CONNECTION_REFUSED"));
        assertTrue(WebsiteService.classifyFailure(
                new ConnectException("Network is unreachable")
        ).startsWith("CONNECTION_FAILED"));
        assertTrue(WebsiteService.classifyFailure(
                new SecurityException("blocked")
        ).startsWith("UNSAFE_TARGET"));
    }

    @Test
    void blocksChecksToPrivateNetworkTargetsByDefault() {
        WebsiteRepository websiteRepository = mock(WebsiteRepository.class);
        MonitoringResultRepository resultRepository = mock(MonitoringResultRepository.class);
        Website website = new Website("Internal", "http://127.0.0.1/health");
        ReflectionTestUtils.setField(website, "id", 1L);
        website.setStatus("UP");
        website.setFailureThreshold(1);
        when(websiteRepository.findById(1L)).thenReturn(Optional.of(website));
        when(websiteRepository.save(website)).thenReturn(website);
        ArgumentCaptor<MonitoringResult> result = ArgumentCaptor.forClass(
                MonitoringResult.class
        );
        WebsiteService service = new WebsiteService(
                websiteRepository,
                resultRepository,
                mock(IncidentRepository.class),
                mock(AlertService.class),
                new MonitoringExecutionProperties(),
                com.yahya.uptime_monitor.TestTransactionOperations.immediate()
        );

        service.checkWebsite(1L);

        verify(resultRepository).save(result.capture());
        assertEquals("DOWN", result.getValue().getStatus());
        assertTrue(result.getValue().getFailureReason().startsWith("UNSAFE_TARGET"));
    }

    @Test
    void updatesPublicVisibilityThroughWebsiteSettings() {
        WebsiteRepository websiteRepository = mock(WebsiteRepository.class);
        MonitoringResultRepository resultRepository = mock(MonitoringResultRepository.class);
        IncidentRepository incidentRepository = mock(IncidentRepository.class);
        AlertService alertService = mock(AlertService.class);
        WebsiteService service = new WebsiteService(
                websiteRepository,
                resultRepository,
                incidentRepository,
                alertService,
                new MonitoringExecutionProperties(),
                com.yahya.uptime_monitor.TestTransactionOperations.immediate()
        );
        Website website = new Website("Status page", "https://example.com");
        WebsiteSettingsRequest settings = new WebsiteSettingsRequest();
        settings.setPubliclyVisible(true);
        settings.setTag("production");

        when(websiteRepository.findById(1L)).thenReturn(Optional.of(website));
        when(websiteRepository.save(website)).thenReturn(website);

        Website updated = service.updateMonitoringSettings(1L, settings);

        assertTrue(updated.isPubliclyVisible());
        assertEquals("production", updated.getTag());
    }

    @Test
    void rejectsAWebsiteWithAnEquivalentDuplicateUrl() {
        WebsiteRepository websiteRepository = mock(WebsiteRepository.class);
        MonitoringResultRepository resultRepository = mock(MonitoringResultRepository.class);
        IncidentRepository incidentRepository = mock(IncidentRepository.class);
        AlertService alertService = mock(AlertService.class);
        WebsiteService service = new WebsiteService(
                websiteRepository,
                resultRepository,
                incidentRepository,
                alertService,
                new MonitoringExecutionProperties(),
                com.yahya.uptime_monitor.TestTransactionOperations.immediate()
        );
        Website website = new Website(" API ", "HTTPS://Example.COM:443/");
        when(websiteRepository.existsByUrl("https://example.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.addWebsite(website)
        );

        assertEquals(409, exception.getStatusCode().value());
        assertEquals("https://example.com", website.getUrl());
        assertEquals("API", website.getName());
        verify(websiteRepository, never()).save(website);
    }

    @Test
    void clearsClientSuppliedMonitoringStateWhenCreatingAWebsite() {
        WebsiteRepository websiteRepository = mock(WebsiteRepository.class);
        WebsiteService service = new WebsiteService(
                websiteRepository,
                mock(MonitoringResultRepository.class),
                mock(IncidentRepository.class),
                mock(AlertService.class),
                new MonitoringExecutionProperties(),
                com.yahya.uptime_monitor.TestTransactionOperations.immediate()
        );
        Website website = new Website("API", "https://example.com");
        website.setStatus("UP");
        website.setConsecutiveFailures(8);
        website.setConsecutiveSuccesses(9);
        website.setLastSuccessfulCheckAt(LocalDateTime.now());
        website.setLastFailedCheckAt(LocalDateTime.now());
        website.setLastFailureReason("client supplied");
        website.setLastHttpStatusCode(200);
        website.setLastResponseTime(1L);
        when(websiteRepository.save(website)).thenReturn(website);

        Website saved = service.addWebsite(website);

        assertNull(saved.getStatus());
        assertEquals(0, saved.getConsecutiveFailures());
        assertEquals(0, saved.getConsecutiveSuccesses());
        assertNull(saved.getLastSuccessfulCheckAt());
        assertNull(saved.getLastFailedCheckAt());
        assertNull(saved.getLastFailureReason());
        assertNull(saved.getLastHttpStatusCode());
        assertNull(saved.getLastResponseTime());
    }
}
