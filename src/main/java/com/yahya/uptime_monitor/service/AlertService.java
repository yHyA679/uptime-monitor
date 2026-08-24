package com.yahya.uptime_monitor.service;

import com.yahya.uptime_monitor.model.AlertChannel;
import com.yahya.uptime_monitor.model.AlertEvent;
import com.yahya.uptime_monitor.model.AlertEventType;
import com.yahya.uptime_monitor.model.AlertSeverity;
import com.yahya.uptime_monitor.model.Website;
import com.yahya.uptime_monitor.notification.NotificationSender;
import com.yahya.uptime_monitor.repository.AlertEventRepository;
import com.yahya.uptime_monitor.repository.WebsiteRepository;
import com.yahya.uptime_monitor.util.PaginationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertEventRepository alertEventRepository;
    private final WebsiteRepository websiteRepository;
    private final Map<AlertChannel, NotificationSender> senders;

    public AlertService(
            AlertEventRepository alertEventRepository,
            WebsiteRepository websiteRepository,
            List<NotificationSender> notificationSenders
    ) {
        this.alertEventRepository = alertEventRepository;
        this.websiteRepository = websiteRepository;
        this.senders = new EnumMap<>(AlertChannel.class);
        notificationSenders.forEach(sender -> this.senders.put(sender.channel(), sender));
    }

    @Transactional
    public AlertEvent recordDowntime(Website website, LocalDateTime createdAt) {
        return record(
                website,
                AlertEventType.DOWNTIME,
                AlertSeverity.CRITICAL,
                website.getName() + " is DOWN",
                createdAt
        );
    }

    @Transactional
    public AlertEvent recordRecovery(Website website, LocalDateTime createdAt) {
        return record(
                website,
                AlertEventType.RECOVERY,
                AlertSeverity.INFO,
                website.getName() + " has recovered and is UP",
                createdAt
        );
    }

    public List<AlertEvent> getAllAlerts() {
        return alertEventRepository.findAllByOrderByCreatedAtDesc();
    }

    public Page<AlertEvent> getAllAlerts(Pageable pageable) {
        Sort sort = Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
        );
        return alertEventRepository.findAll(PaginationSupport.withSort(pageable, sort));
    }

    public List<AlertEvent> getAlertsForWebsite(Long websiteId) {
        if (!websiteRepository.existsById(websiteId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Website with id " + websiteId + " was not found"
            );
        }

        return alertEventRepository.findByWebsiteIdOrderByCreatedAtDesc(websiteId);
    }

    public Page<AlertEvent> getAlertsForWebsite(Long websiteId, Pageable pageable) {
        if (!websiteRepository.existsById(websiteId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Website with id " + websiteId + " was not found"
            );
        }

        Sort sort = Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
        );
        return alertEventRepository.findByWebsiteId(
                websiteId,
                PaginationSupport.withSort(pageable, sort)
        );
    }

    @Transactional
    public void deleteAlertsForWebsite(Long websiteId) {
        alertEventRepository.deleteByWebsiteId(websiteId);
    }

    private AlertEvent record(
            Website website,
            AlertEventType type,
            AlertSeverity severity,
            String message,
            LocalDateTime createdAt
    ) {
        AlertEvent alertEvent = new AlertEvent();
        alertEvent.setWebsite(website);
        alertEvent.setType(type);
        alertEvent.setSeverity(severity);
        alertEvent.setChannel(AlertChannel.EMAIL);
        alertEvent.setMessage(message);
        alertEvent.setCreatedAt(createdAt);

        AlertEvent savedEvent = alertEventRepository.save(alertEvent);
        notify(savedEvent);
        return savedEvent;
    }

    private void notify(AlertEvent alertEvent) {
        NotificationSender sender = senders.get(alertEvent.getChannel());

        if (sender == null) {
            log.warn("No notification sender is registered for channel {}", alertEvent.getChannel());
            return;
        }

        try {
            sender.send(alertEvent);
        } catch (RuntimeException exception) {
            log.error(
                    "Could not send alert event {} through {}",
                    alertEvent.getId(),
                    alertEvent.getChannel(),
                    exception
            );
        }
    }
}
