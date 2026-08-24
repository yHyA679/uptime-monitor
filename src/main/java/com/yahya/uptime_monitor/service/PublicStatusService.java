package com.yahya.uptime_monitor.service;

import com.yahya.uptime_monitor.dto.PublicIncidentResponse;
import com.yahya.uptime_monitor.dto.PublicStatusPageResponse;
import com.yahya.uptime_monitor.dto.PublicWebsiteStatusResponse;
import com.yahya.uptime_monitor.dto.WebsiteStats;
import com.yahya.uptime_monitor.model.Incident;
import com.yahya.uptime_monitor.model.Website;
import com.yahya.uptime_monitor.repository.IncidentRepository;
import com.yahya.uptime_monitor.repository.WebsiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PublicStatusService {

    private final WebsiteRepository websiteRepository;
    private final IncidentRepository incidentRepository;
    private final WebsiteService websiteService;

    public PublicStatusService(
            WebsiteRepository websiteRepository,
            IncidentRepository incidentRepository,
            WebsiteService websiteService
    ) {
        this.websiteRepository = websiteRepository;
        this.incidentRepository = incidentRepository;
        this.websiteService = websiteService;
    }

    @Transactional(readOnly = true)
    public PublicStatusPageResponse getPublicStatus() {
        List<PublicWebsiteStatusResponse> websites = websiteRepository
                .findByPubliclyVisibleTrueOrderByNameAsc()
                .stream()
                .map(this::toPublicStatus)
                .toList();

        return new PublicStatusPageResponse(LocalDateTime.now(), websites);
    }

    private PublicWebsiteStatusResponse toPublicStatus(Website website) {
        WebsiteStats stats = websiteService.getStats(website.getId());
        List<PublicIncidentResponse> incidents = incidentRepository
                .findTop5ByWebsiteIdOrderByStartedAtDesc(website.getId())
                .stream()
                .map(this::toPublicIncident)
                .toList();

        return new PublicWebsiteStatusResponse(
                website.getName(),
                website.getStatus() == null ? "PENDING" : website.getStatus(),
                stats.getUptimePercentage(),
                stats.getLastCheckedAt(),
                incidents
        );
    }

    private PublicIncidentResponse toPublicIncident(Incident incident) {
        return new PublicIncidentResponse(
                incident.getStartedAt(),
                incident.getResolvedAt(),
                incident.getDurationSeconds(),
                incident.getStatus()
        );
    }
}
