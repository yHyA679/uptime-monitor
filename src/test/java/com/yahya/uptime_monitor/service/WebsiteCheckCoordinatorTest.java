package com.yahya.uptime_monitor.service;

import com.yahya.uptime_monitor.model.Website;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebsiteCheckCoordinatorTest {

    @Test
    void preventsOverlappingScheduledAndManualChecksForTheSameWebsite() throws Exception {
        WebsiteService websiteService = mock(WebsiteService.class);
        ThreadPoolExecutor executor = executorWithOneWorkerAndOneQueueSlot();
        WebsiteCheckCoordinator coordinator = new WebsiteCheckCoordinator(websiteService, executor);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        when(websiteService.checkWebsite(1L)).thenAnswer(invocation -> {
            started.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return new Website("API", "https://example.com");
        });

        try {
            assertEquals(
                    WebsiteCheckCoordinator.SubmissionResult.SUBMITTED,
                    coordinator.submitScheduledCheck(1L)
            );
            assertTrue(started.await(5, TimeUnit.SECONDS));
            assertEquals(
                    WebsiteCheckCoordinator.SubmissionResult.ALREADY_IN_PROGRESS,
                    coordinator.submitScheduledCheck(1L)
            );

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> coordinator.checkNow(1L)
            );
            assertEquals(409, exception.getStatusCode().value());
        } finally {
            release.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        verify(websiteService, times(1)).checkWebsite(1L);
    }

    @Test
    void rejectsNewWorkWhenTheBoundedQueueIsFull() throws Exception {
        WebsiteService websiteService = mock(WebsiteService.class);
        ThreadPoolExecutor executor = executorWithOneWorkerAndOneQueueSlot();
        WebsiteCheckCoordinator coordinator = new WebsiteCheckCoordinator(websiteService, executor);
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);

        when(websiteService.checkWebsite(1L)).thenAnswer(invocation -> {
            firstStarted.countDown();
            assertTrue(releaseFirst.await(5, TimeUnit.SECONDS));
            return new Website();
        });
        when(websiteService.checkWebsite(2L)).thenReturn(new Website());

        try {
            assertEquals(
                    WebsiteCheckCoordinator.SubmissionResult.SUBMITTED,
                    coordinator.submitScheduledCheck(1L)
            );
            assertTrue(firstStarted.await(5, TimeUnit.SECONDS));
            assertEquals(
                    WebsiteCheckCoordinator.SubmissionResult.SUBMITTED,
                    coordinator.submitScheduledCheck(2L)
            );
            assertEquals(
                    WebsiteCheckCoordinator.SubmissionResult.CAPACITY_REACHED,
                    coordinator.submitScheduledCheck(3L)
            );
        } finally {
            releaseFirst.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private ThreadPoolExecutor executorWithOneWorkerAndOneQueueSlot() {
        return new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
