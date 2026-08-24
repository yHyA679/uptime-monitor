package com.yahya.uptime_monitor.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "alert_events",
        indexes = @Index(
                name = "idx_alert_events_website_created_at",
                columnList = "website_id, created_at"
        )
)
public class AlertEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "website_id", nullable = false)
    private Website website;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertEventType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertChannel channel;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public AlertEvent() {
    }

    public Long getId() {
        return id;
    }

    public Website getWebsite() {
        return website;
    }

    public void setWebsite(Website website) {
        this.website = website;
    }

    public AlertEventType getType() {
        return type;
    }

    public void setType(AlertEventType type) {
        this.type = type;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AlertSeverity severity) {
        this.severity = severity;
    }

    public AlertChannel getChannel() {
        return channel;
    }

    public void setChannel(AlertChannel channel) {
        this.channel = channel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
