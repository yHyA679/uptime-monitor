package com.yahya.uptime_monitor.repository;

import java.time.LocalDateTime;

public interface WebsiteMetricsProjection {

    Long getWebsiteId();

    Double getUptimePercentage();

    Double getAverageResponseTime();

    LocalDateTime getLastCheckedAt();
}
