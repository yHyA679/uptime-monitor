package com.yahya.uptime_monitor.service;

import com.yahya.uptime_monitor.model.Website;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

@Service
public class WebsiteCheckCoordinator {

    private static final Logger log = LoggerFactory.getLogger(WebsiteCheckCoordinator.class);

    private final WebsiteService websiteService;
    private final ExecutorService monitoringExecutor;
    private final Set<Long> checksInProgress = ConcurrentHashMap.newKeySet();

    public WebsiteCheckCoordinator(
            WebsiteService websiteService,
            @Qualifier("monitoringExecutor") ExecutorService monitoringExecutor
    ) {
        this.websiteService = websiteService;
        this.monitoringExecutor = monitoringExecutor;
    }

    public Website checkNow(Long websiteId) {
        if (!checksInProgress.add(websiteId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A check is already in progress for website " + websiteId
            );
        }

        try {
            return websiteService.checkWebsite(websiteId);
        } finally {
            checksInProgress.remove(websiteId);
        }
    }

    public SubmissionResult submitScheduledCheck(Long websiteId) {
        if (!checksInProgress.add(websiteId)) {
            return SubmissionResult.ALREADY_IN_PROGRESS;
        }

        try {
            monitoringExecutor.execute(() -> runScheduledCheck(websiteId));
            return SubmissionResult.SUBMITTED;
        } catch (RejectedExecutionException exception) {
            checksInProgress.remove(websiteId);
            return SubmissionResult.CAPACITY_REACHED;
        }
    }

    private void runScheduledCheck(Long websiteId) {
        try {
            websiteService.checkWebsite(websiteId);
        } catch (RuntimeException exception) {
            log.error("Scheduled check failed unexpectedly for website {}", websiteId, exception);
        } finally {
            checksInProgress.remove(websiteId);
        }
    }

    public enum SubmissionResult {
        SUBMITTED,
        ALREADY_IN_PROGRESS,
        CAPACITY_REACHED
    }
}
