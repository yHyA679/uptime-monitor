package com.yahya.uptime_monitor.service;

import com.yahya.uptime_monitor.model.Website;
import com.yahya.uptime_monitor.repository.WebsiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MonitoringScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonitoringScheduler.class);

    private final WebsiteRepository websiteRepository;
    private final WebsiteCheckCoordinator checkCoordinator;
    private final AtomicBoolean scanInProgress = new AtomicBoolean(false);

    public MonitoringScheduler(
            WebsiteRepository websiteRepository,
            WebsiteCheckCoordinator checkCoordinator
    ) {
        this.websiteRepository = websiteRepository;
        this.checkCoordinator = checkCoordinator;
    }

    @Scheduled(fixedDelayString = "${monitoring.scheduler-delay-ms:10000}")
    public void scheduleDueChecks() {
        if (!scanInProgress.compareAndSet(false, true)) {
            log.debug("Skipping monitoring scan because the previous scan is still running");
            return;
        }

        try {
            LocalDateTime now = LocalDateTime.now();

            for (Website website : websiteRepository.findByEnabledTrue()) {
                if (!isDueForCheck(website, now)) {
                    continue;
                }

                WebsiteCheckCoordinator.SubmissionResult result =
                        checkCoordinator.submitScheduledCheck(website.getId());

                if (result == WebsiteCheckCoordinator.SubmissionResult.CAPACITY_REACHED) {
                    log.warn("Monitoring worker queue is full; remaining websites will retry next scan");
                    break;
                }
            }
        } finally {
            scanInProgress.set(false);
        }
    }

    private boolean isDueForCheck(Website website, LocalDateTime now) {
        LocalDateTime lastCheckedAt = latest(
                website.getLastSuccessfulCheckAt(),
                website.getLastFailedCheckAt()
        );

        return lastCheckedAt == null
                || !lastCheckedAt.plusSeconds(website.getCheckIntervalSeconds()).isAfter(now);
    }

    private LocalDateTime latest(LocalDateTime first, LocalDateTime second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isAfter(second) ? first : second;
    }
}
