package com.yahya.uptime_monitor.repository;

import com.yahya.uptime_monitor.config.MonitoringExecutionProperties;
import com.yahya.uptime_monitor.model.AlertChannel;
import com.yahya.uptime_monitor.model.AlertEvent;
import com.yahya.uptime_monitor.model.AlertEventType;
import com.yahya.uptime_monitor.model.AlertSeverity;
import com.yahya.uptime_monitor.model.Incident;
import com.yahya.uptime_monitor.model.MonitoringResult;
import com.yahya.uptime_monitor.model.Website;
import com.yahya.uptime_monitor.service.AlertService;
import com.yahya.uptime_monitor.service.MonitoringHistoryRetentionService;
import com.yahya.uptime_monitor.service.WebsiteService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class MonitoringPersistenceIntegrationTest {

    @Autowired
    private WebsiteRepository websiteRepository;

    @Autowired
    private MonitoringResultRepository monitoringResultRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private AlertEventRepository alertEventRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void createsAndNormalizesAWebsiteUsingTheRealRepository() {
        Website saved = websiteService().addWebsite(new Website(
                " Production API ",
                "HTTPS://Example.COM:443/"
        ));
        entityManager.flush();
        entityManager.clear();

        Website reloaded = websiteRepository.findById(saved.getId()).orElseThrow();
        assertEquals("Production API", reloaded.getName());
        assertEquals("https://example.com", reloaded.getUrl());

        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> websiteService().addWebsite(new Website(
                        "Duplicate",
                        "https://example.com/"
                ))
        );
    }

    @Test
    void databaseConstraintRejectsAnExactDuplicateUrl() {
        websiteRepository.saveAndFlush(new Website("First", "https://example.com"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> websiteRepository.saveAndFlush(
                        new Website("Second", "https://example.com")
                )
        );
    }

    @Test
    void persistsHistoryAndCalculatesAggregateMetrics() {
        Website website = websiteRepository.saveAndFlush(
                new Website("Metrics", "https://metrics.example.com")
        );
        LocalDateTime firstCheck = LocalDateTime.of(2026, 8, 24, 10, 0);
        LocalDateTime lastCheck = firstCheck.plusMinutes(2);
        monitoringResultRepository.saveAllAndFlush(List.of(
                result(website, "UP", 100, firstCheck),
                result(website, "DOWN", 300, firstCheck.plusMinutes(1)),
                result(website, "UP", 200, lastCheck)
        ));
        entityManager.clear();

        List<MonitoringResult> history = monitoringResultRepository
                .findByWebsiteIdOrderByCheckedAtDesc(website.getId());
        WebsiteMetricsProjection metrics = monitoringResultRepository
                .findMetricsByWebsiteIds(List.of(website.getId()))
                .get(0);
        WebsiteStatsProjection stats = monitoringResultRepository
                .findStatsByWebsiteId(website.getId());

        assertEquals(3, history.size());
        assertEquals(lastCheck, history.get(0).getCheckedAt());
        assertEquals(200.0, metrics.getAverageResponseTime());
        assertEquals(2.0 / 3.0 * 100.0, metrics.getUptimePercentage(), 0.001);
        assertEquals(lastCheck, metrics.getLastCheckedAt());
        assertEquals(3, stats.getTotalChecks());
        assertEquals(2, stats.getUpChecks());
        assertEquals(1, stats.getDownChecks());
        assertEquals(200.0, stats.getAverageResponseTime());
        assertEquals(lastCheck, stats.getLastCheckedAt());
    }

    @Test
    void calculatesDashboardMetricsFromMonitoringHistory() {
        Website website = websiteRepository.saveAndFlush(
                new Website("Dashboard", "https://dashboard.example.com")
        );
        LocalDateTime firstCheck = LocalDateTime.of(2026, 8, 24, 10, 0);
        monitoringResultRepository.saveAllAndFlush(List.of(
                result(website, "UP", 100, firstCheck),
                result(website, "UP", 200, firstCheck.plusMinutes(1)),
                result(website, "DOWN", 300, firstCheck.plusMinutes(2))
        ));

        DashboardMetricsProjection metrics =
                monitoringResultRepository.findDashboardMetrics();

        assertEquals(66.666, metrics.getAverageUptime(), 0.001);
        assertEquals(200.0, metrics.getAverageResponseTime());
        assertEquals(
                2,
                monitoringResultRepository.countByCheckedAtGreaterThanEqual(
                        firstCheck.plusMinutes(1)
                )
        );
    }

    @Test
    void websiteDeletionRemovesOnlyItsRelatedHistoryIncidentsAndAlerts() {
        Website deletedWebsite = websiteRepository.save(
                new Website("Delete me", "https://delete.example.com")
        );
        Website retainedWebsite = websiteRepository.save(
                new Website("Keep me", "https://keep.example.com")
        );
        LocalDateTime checkedAt = LocalDateTime.of(2026, 8, 24, 12, 0);

        monitoringResultRepository.save(result(deletedWebsite, "DOWN", 500, checkedAt));
        monitoringResultRepository.save(result(retainedWebsite, "UP", 100, checkedAt));
        incidentRepository.save(incident(deletedWebsite, checkedAt));
        incidentRepository.save(incident(retainedWebsite, checkedAt));
        alertEventRepository.save(alert(deletedWebsite, checkedAt));
        alertEventRepository.save(alert(retainedWebsite, checkedAt));
        entityManager.flush();

        websiteService().deleteWebsite(deletedWebsite.getId());
        entityManager.flush();
        entityManager.clear();

        assertFalse(websiteRepository.existsById(deletedWebsite.getId()));
        assertTrue(websiteRepository.existsById(retainedWebsite.getId()));
        assertTrue(monitoringResultRepository
                .findByWebsiteIdOrderByCheckedAtDesc(deletedWebsite.getId()).isEmpty());
        assertTrue(incidentRepository
                .findByWebsiteIdOrderByStartedAtDesc(deletedWebsite.getId()).isEmpty());
        assertTrue(alertEventRepository
                .findByWebsiteIdOrderByCreatedAtDesc(deletedWebsite.getId()).isEmpty());
        assertEquals(1, monitoringResultRepository
                .findByWebsiteIdOrderByCheckedAtDesc(retainedWebsite.getId()).size());
        assertEquals(1, incidentRepository
                .findByWebsiteIdOrderByStartedAtDesc(retainedWebsite.getId()).size());
        assertEquals(1, alertEventRepository
                .findByWebsiteIdOrderByCreatedAtDesc(retainedWebsite.getId()).size());
    }

    @Test
    void retentionDeletesOnlyExpiredMonitoringResults() {
        Website website = websiteRepository.saveAndFlush(
                new Website("Retention", "https://retention.example.com")
        );
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-24T12:00:00Z"),
                ZoneOffset.UTC
        );
        MonitoringExecutionProperties properties = new MonitoringExecutionProperties();
        LocalDateTime oldCheck = LocalDateTime.of(2026, 7, 1, 12, 0);
        LocalDateTime recentCheck = LocalDateTime.of(2026, 8, 20, 12, 0);
        monitoringResultRepository.save(result(website, "UP", 100, oldCheck));
        monitoringResultRepository.save(result(website, "UP", 120, recentCheck));
        incidentRepository.save(incident(website, oldCheck));
        alertEventRepository.save(alert(website, oldCheck));
        entityManager.flush();

        new MonitoringHistoryRetentionService(
                monitoringResultRepository,
                properties,
                clock
        ).cleanupExpiredHistory();
        entityManager.flush();
        entityManager.clear();

        List<MonitoringResult> remaining = monitoringResultRepository
                .findByWebsiteIdOrderByCheckedAtDesc(website.getId());
        assertEquals(1, remaining.size());
        assertEquals(recentCheck, remaining.get(0).getCheckedAt());
        assertEquals(1, incidentRepository
                .findByWebsiteIdOrderByStartedAtDesc(website.getId()).size());
        assertEquals(1, alertEventRepository
                .findByWebsiteIdOrderByCreatedAtDesc(website.getId()).size());
    }

    private WebsiteService websiteService() {
        AlertService alertService = new AlertService(
                alertEventRepository,
                websiteRepository,
                List.of()
        );
        return new WebsiteService(
                websiteRepository,
                monitoringResultRepository,
                incidentRepository,
                alertService,
                new MonitoringExecutionProperties(),
                com.yahya.uptime_monitor.TestTransactionOperations.immediate()
        );
    }

    private MonitoringResult result(
            Website website,
            String status,
            long responseTime,
            LocalDateTime checkedAt
    ) {
        MonitoringResult result = new MonitoringResult();
        result.setWebsite(website);
        result.setStatus(status);
        result.setResponseTime(responseTime);
        result.setCheckedAt(checkedAt);
        return result;
    }

    private Incident incident(Website website, LocalDateTime startedAt) {
        Incident incident = new Incident();
        incident.setWebsite(website);
        incident.setStartedAt(startedAt);
        incident.setStatus("OPEN");
        return incident;
    }

    private AlertEvent alert(Website website, LocalDateTime createdAt) {
        AlertEvent alert = new AlertEvent();
        alert.setWebsite(website);
        alert.setType(AlertEventType.DOWNTIME);
        alert.setSeverity(AlertSeverity.CRITICAL);
        alert.setChannel(AlertChannel.EMAIL);
        alert.setMessage(website.getName() + " is DOWN");
        alert.setCreatedAt(createdAt);
        return alert;
    }
}
