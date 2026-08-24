package com.yahya.uptime_monitor.repository;

import java.time.LocalDateTime;

public interface WebsiteStatsProjection {

    Long getTotalChecks();

    Long getUpChecks();

    Long getDownChecks();

    Double getAverageResponseTime();

    LocalDateTime getLastCheckedAt();
}
