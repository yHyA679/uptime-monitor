package com.yahya.uptime_monitor.repository;

public interface DashboardMetricsProjection {

    Double getAverageUptime();

    Double getAverageResponseTime();
}
