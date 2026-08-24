package com.yahya.uptime_monitor.service;

import com.yahya.uptime_monitor.dto.DashboardSummary;
import com.yahya.uptime_monitor.repository.DashboardMetricsProjection;
import com.yahya.uptime_monitor.repository.IncidentRepository;
import com.yahya.uptime_monitor.repository.MonitoringResultRepository;
import com.yahya.uptime_monitor.repository.WebsiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class DashboardService {

    private final WebsiteRepository websiteRepository;
    private final MonitoringResultRepository monitoringResultRepository;
    private final IncidentRepository incidentRepository;
    private final Clock clock;

    public DashboardService(
            WebsiteRepository websiteRepository,
            MonitoringResultRepository monitoringResultRepository,
            IncidentRepository incidentRepository,
            Clock clock
    ) {
        this.websiteRepository = websiteRepository;
        this.monitoringResultRepository = monitoringResultRepository;
        this.incidentRepository = incidentRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardSummary getSummary() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusHours(24);
        DashboardMetricsProjection metrics = monitoringResultRepository.findDashboardMetrics();
        Double averageUptime = metrics == null ? null : metrics.getAverageUptime();
        Double averageResponseTime = metrics == null ? null : metrics.getAverageResponseTime();

        return new DashboardSummary(
                websiteRepository.count(),
                websiteRepository.countByEnabledTrueAndStatus("UP"),
                websiteRepository.countByEnabledTrueAndStatus("DOWN"),
                websiteRepository.countByEnabledFalse(),
                roundPercentage(valueOrZero(averageUptime)),
                Math.round(valueOrZero(averageResponseTime)),
                incidentRepository.countByStatus("OPEN"),
                incidentRepository.countByStartedAtGreaterThanEqual(cutoff),
                monitoringResultRepository.countByCheckedAtGreaterThanEqual(cutoff)
        );
    }

    private double valueOrZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private double roundPercentage(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
