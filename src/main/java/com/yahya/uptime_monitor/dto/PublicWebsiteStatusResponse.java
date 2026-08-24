package com.yahya.uptime_monitor.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PublicWebsiteStatusResponse(
        String name,
        String status,
        double uptimePercentage,
        LocalDateTime lastCheckedAt,
        List<PublicIncidentResponse> recentIncidents
) {
}
