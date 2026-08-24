package com.yahya.uptime_monitor.service;

import com.yahya.uptime_monitor.dto.PublicIncidentResponse;
import com.yahya.uptime_monitor.dto.PublicStatusPageResponse;
import com.yahya.uptime_monitor.dto.PublicWebsiteStatusResponse;
import com.yahya.uptime_monitor.dto.WebsiteStats;
import com.yahya.uptime_monitor.model.Incident;
import com.yahya.uptime_monitor.model.Website;
import com.yahya.uptime_monitor.repository.IncidentRepository;
import com.yahya.uptime_monitor.repository.WebsiteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicStatusServiceTest {

    @Test
    void returnsOnlyTheIntendedPublicStatusFields() {
        WebsiteRepository websiteRepository = mock(WebsiteRepository.class);
        IncidentRepository incidentRepository = mock(IncidentRepository.class);
        WebsiteService websiteService = mock(WebsiteService.class);
        PublicStatusService publicStatusService = new PublicStatusService(
                websiteRepository,
                incidentRepository,
                websiteService
        );

        Website website = new Website("Public API", "https://internal.example.com/health");
        ReflectionTestUtils.setField(website, "id", 42L);
        website.setPubliclyVisible(true);
        website.setStatus("UP");

        LocalDateTime checkedAt = LocalDateTime.of(2026, 8, 24, 12, 0);
        LocalDateTime incidentStartedAt = checkedAt.minusMinutes(10);
        Incident incident = new Incident();
        incident.setWebsite(website);
        incident.setStartedAt(incidentStartedAt);
        incident.setResolvedAt(checkedAt.minusMinutes(5));
        incident.setDurationSeconds(300L);
        incident.setStatus("RESOLVED");

        when(websiteRepository.findByPubliclyVisibleTrueOrderByNameAsc())
                .thenReturn(List.of(website));
        when(websiteService.getStats(42L)).thenReturn(new WebsiteStats(
                42L,
                100,
                99,
                1,
                99.0,
                150,
                checkedAt
        ));
        when(incidentRepository.findTop5ByWebsiteIdOrderByStartedAtDesc(42L))
                .thenReturn(List.of(incident));

        PublicStatusPageResponse response = publicStatusService.getPublicStatus();
        PublicWebsiteStatusResponse publicWebsite = response.websites().get(0);

        assertNotNull(response.generatedAt());
        assertEquals("Public API", publicWebsite.name());
        assertEquals("UP", publicWebsite.status());
        assertEquals(99.0, publicWebsite.uptimePercentage());
        assertEquals(checkedAt, publicWebsite.lastCheckedAt());
        assertEquals(1, publicWebsite.recentIncidents().size());
        assertEquals("RESOLVED", publicWebsite.recentIncidents().get(0).status());

        List<String> websiteFields = Arrays.stream(
                        PublicWebsiteStatusResponse.class.getRecordComponents()
                )
                .map(component -> component.getName())
                .toList();
        List<String> incidentFields = Arrays.stream(
                        PublicIncidentResponse.class.getRecordComponents()
                )
                .map(component -> component.getName())
                .toList();

        assertEquals(
                List.of("name", "status", "uptimePercentage", "lastCheckedAt", "recentIncidents"),
                websiteFields
        );
        assertEquals(
                List.of("startedAt", "resolvedAt", "durationSeconds", "status"),
                incidentFields
        );
        assertFalse(websiteFields.contains("id"));
        assertFalse(websiteFields.contains("url"));
    }
}
