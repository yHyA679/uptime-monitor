package com.yahya.uptime_monitor.notification;

import com.yahya.uptime_monitor.model.AlertChannel;
import com.yahya.uptime_monitor.model.AlertEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

    @Override
    public AlertChannel channel() {
        return AlertChannel.EMAIL;
    }

    @Override
    public void send(AlertEvent alertEvent) {
        log.info(
                "EMAIL notification placeholder: alert event {} recorded for website {}. SMTP is not configured.",
                alertEvent.getId(),
                alertEvent.getWebsite().getId()
        );
    }
}
