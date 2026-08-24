package com.yahya.uptime_monitor.dto;

public record DashboardSummary(
        long totalWebsites,
        long upWebsites,
        long downWebsites,
        long disabledWebsites,
        double averageUptime,
        long averageResponseTime,
        long openIncidents,
        long recentIncidentCount,
        long checksLast24Hours
) {
}
