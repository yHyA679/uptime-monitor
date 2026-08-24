package com.yahya.uptime_monitor.service;

import com.yahya.uptime_monitor.config.MonitoringExecutionProperties;
import com.yahya.uptime_monitor.repository.MonitoringResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class MonitoringHistoryRetentionService {

    private static final Logger log = LoggerFactory.getLogger(
            MonitoringHistoryRetentionService.class
    );

    private final MonitoringResultRepository monitoringResultRepository;
    private final MonitoringExecutionProperties properties;
    private final Clock clock;

    public MonitoringHistoryRetentionService(
            MonitoringResultRepository monitoringResultRepository,
            MonitoringExecutionProperties properties,
            Clock clock
    ) {
        this.monitoringResultRepository = monitoringResultRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString = "${monitoring.retention-cleanup-delay-ms:86400000}",
            initialDelayString = "60000"
    )
    @Transactional
    public void cleanupExpiredHistory() {
        LocalDateTime cutoff = LocalDateTime.now(clock)
                .minusDays(properties.getRetentionDays());
        int deletedResults = monitoringResultRepository.deleteExpiredBefore(cutoff);

        if (deletedResults > 0) {
            log.info(
                    "Deleted {} monitoring results older than {} days",
                    deletedResults,
                    properties.getRetentionDays()
            );
        }
    }
}
