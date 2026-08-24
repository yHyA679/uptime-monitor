package com.yahya.uptime_monitor.dto;

import java.time.LocalDateTime;

public class WebsiteStats {

    private final Long websiteId;
    private final long totalChecks;
    private final long upChecks;
    private final long downChecks;
    private final double uptimePercentage;
    private final long averageResponseTime;
    private final LocalDateTime lastCheckedAt;

    public WebsiteStats(
            Long websiteId,
            long totalChecks,
            long upChecks,
            long downChecks,
            double uptimePercentage,
            long averageResponseTime,
            LocalDateTime lastCheckedAt
    ) {
        this.websiteId = websiteId;
        this.totalChecks = totalChecks;
        this.upChecks = upChecks;
        this.downChecks = downChecks;
        this.uptimePercentage = uptimePercentage;
        this.averageResponseTime = averageResponseTime;
        this.lastCheckedAt = lastCheckedAt;
    }

    public Long getWebsiteId() {
        return websiteId;
    }

    public long getTotalChecks() {
        return totalChecks;
    }

    public long getUpChecks() {
        return upChecks;
    }

    public long getDownChecks() {
        return downChecks;
    }

    public double getUptimePercentage() {
        return uptimePercentage;
    }

    public long getAverageResponseTime() {
        return averageResponseTime;
    }

    public LocalDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }
}
