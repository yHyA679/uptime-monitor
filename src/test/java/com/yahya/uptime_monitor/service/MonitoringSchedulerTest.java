package com.yahya.uptime_monitor.service;

import com.yahya.uptime_monitor.model.Website;
import com.yahya.uptime_monitor.repository.WebsiteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonitoringSchedulerTest {

    @Test
    void doesNotSubmitAWebsiteBeforeItsConfiguredIntervalIsDue() {
        WebsiteRepository websiteRepository = mock(WebsiteRepository.class);
        WebsiteCheckCoordinator coordinator = mock(WebsiteCheckCoordinator.class);
        MonitoringScheduler scheduler = new MonitoringScheduler(websiteRepository, coordinator);
        Website website = website(1L);
        website.setCheckIntervalSeconds(60);
        website.setLastSuccessfulCheckAt(LocalDateTime.now());
        when(websiteRepository.findByEnabledTrue()).thenReturn(List.of(website));

        scheduler.scheduleDueChecks();

        verify(coordinator, never()).submitScheduledCheck(1L);
    }

    @Test
    void stopsSubmittingWhenWorkerCapacityIsReached() {
        WebsiteRepository websiteRepository = mock(WebsiteRepository.class);
        WebsiteCheckCoordinator coordinator = mock(WebsiteCheckCoordinator.class);
        MonitoringScheduler scheduler = new MonitoringScheduler(
                websiteRepository,
                coordinator
        );
        Website first = website(1L);
        Website second = website(2L);
        Website third = website(3L);

        when(websiteRepository.findByEnabledTrue()).thenReturn(List.of(first, second, third));
        when(coordinator.submitScheduledCheck(1L))
                .thenReturn(WebsiteCheckCoordinator.SubmissionResult.SUBMITTED);
        when(coordinator.submitScheduledCheck(2L))
                .thenReturn(WebsiteCheckCoordinator.SubmissionResult.CAPACITY_REACHED);

        scheduler.scheduleDueChecks();

        verify(coordinator).submitScheduledCheck(1L);
        verify(coordinator).submitScheduledCheck(2L);
        verify(coordinator, never()).submitScheduledCheck(3L);
    }

    @Test
    void skipsAConcurrentSchedulerScan() throws Exception {
        WebsiteRepository websiteRepository = mock(WebsiteRepository.class);
        WebsiteCheckCoordinator coordinator = mock(WebsiteCheckCoordinator.class);
        MonitoringScheduler scheduler = new MonitoringScheduler(
                websiteRepository,
                coordinator
        );
        CountDownLatch firstScanStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstScan = new CountDownLatch(1);

        when(websiteRepository.findByEnabledTrue()).thenAnswer(invocation -> {
            firstScanStarted.countDown();
            assertTrue(releaseFirstScan.await(5, TimeUnit.SECONDS));
            return List.of();
        });

        Thread firstScan = new Thread(scheduler::scheduleDueChecks);
        firstScan.start();
        assertTrue(firstScanStarted.await(5, TimeUnit.SECONDS));

        scheduler.scheduleDueChecks();
        releaseFirstScan.countDown();
        firstScan.join(5_000);

        verify(websiteRepository, times(1)).findByEnabledTrue();
    }

    private Website website(Long id) {
        Website website = new Website("Website " + id, "https://example.com/" + id);
        ReflectionTestUtils.setField(website, "id", id);
        return website;
    }
}
