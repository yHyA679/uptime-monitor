package com.yahya.uptime_monitor.service;

import com.yahya.uptime_monitor.model.AlertChannel;
import com.yahya.uptime_monitor.model.AlertEvent;
import com.yahya.uptime_monitor.model.AlertEventType;
import com.yahya.uptime_monitor.model.AlertSeverity;
import com.yahya.uptime_monitor.model.Website;
import com.yahya.uptime_monitor.notification.NotificationSender;
import com.yahya.uptime_monitor.repository.AlertEventRepository;
import com.yahya.uptime_monitor.repository.WebsiteRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlertServiceTest {

    @Test
    void storesDowntimeAndRecoveryEventsThenUsesTheEmailSender() {
        AlertEventRepository alertEventRepository = mock(AlertEventRepository.class);
        WebsiteRepository websiteRepository = mock(WebsiteRepository.class);
        NotificationSender emailSender = mock(NotificationSender.class);
        when(emailSender.channel()).thenReturn(AlertChannel.EMAIL);
        when(alertEventRepository.save(any(AlertEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AlertService alertService = new AlertService(
                alertEventRepository,
                websiteRepository,
                List.of(emailSender)
        );
        Website website = new Website("API", "https://example.com/health");
        LocalDateTime downtimeAt = LocalDateTime.of(2026, 8, 24, 10, 0);
        LocalDateTime recoveryAt = downtimeAt.plusMinutes(3);

        AlertEvent downtime = alertService.recordDowntime(website, downtimeAt);
        AlertEvent recovery = alertService.recordRecovery(website, recoveryAt);

        assertEquals(AlertEventType.DOWNTIME, downtime.getType());
        assertEquals(AlertSeverity.CRITICAL, downtime.getSeverity());
        assertEquals(AlertChannel.EMAIL, downtime.getChannel());
        assertEquals(downtimeAt, downtime.getCreatedAt());
        assertEquals("API is DOWN", downtime.getMessage());

        assertEquals(AlertEventType.RECOVERY, recovery.getType());
        assertEquals(AlertSeverity.INFO, recovery.getSeverity());
        assertEquals(recoveryAt, recovery.getCreatedAt());
        assertEquals("API has recovered and is UP", recovery.getMessage());

        verify(alertEventRepository, times(2)).save(any(AlertEvent.class));
        verify(emailSender, times(2)).send(any(AlertEvent.class));
    }
}
