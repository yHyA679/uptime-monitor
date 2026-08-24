package com.yahya.uptime_monitor.notification;

import com.yahya.uptime_monitor.model.AlertChannel;
import com.yahya.uptime_monitor.model.AlertEvent;

public interface NotificationSender {

    AlertChannel channel();

    void send(AlertEvent alertEvent);
}
