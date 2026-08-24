package com.yahya.uptime_monitor.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.yahya.uptime_monitor.validation.ValidWebsiteUrl;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "websites",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_websites_url",
                columnNames = "url"
        )
)
public class Website {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank
    @Size(max = 2048)
    @ValidWebsiteUrl
    @Column(nullable = false, length = 2048)
    private String url;

    private String status;

    @Size(max = 50)
    @Column(length = 50)
    private String tag;

    @Min(10)
    @Max(3600)
    @Column(nullable = false, columnDefinition = "integer default 60")
    private int checkIntervalSeconds = 60;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean enabled = true;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean publiclyVisible = false;

    @Min(1)
    @Max(10)
    @Column(nullable = false, columnDefinition = "integer default 3")
    private int failureThreshold = 3;

    @Min(1)
    @Max(10)
    @Column(nullable = false, columnDefinition = "integer default 2")
    private int recoveryThreshold = 2;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int consecutiveFailures = 0;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int consecutiveSuccesses = 0;

    private LocalDateTime lastSuccessfulCheckAt;

    private LocalDateTime lastFailedCheckAt;

    @Column(length = 1000)
    private String lastFailureReason;

    private Integer lastHttpStatusCode;

    private Long lastResponseTime;

    public Website() {
    }

    public Website(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    public void setCheckIntervalSeconds(int checkIntervalSeconds) {
        this.checkIntervalSeconds = checkIntervalSeconds;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPubliclyVisible() {
        return publiclyVisible;
    }

    public void setPubliclyVisible(boolean publiclyVisible) {
        this.publiclyVisible = publiclyVisible;
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public int getRecoveryThreshold() {
        return recoveryThreshold;
    }

    public void setRecoveryThreshold(int recoveryThreshold) {
        this.recoveryThreshold = recoveryThreshold;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public void setConsecutiveFailures(int consecutiveFailures) {
        this.consecutiveFailures = consecutiveFailures;
    }

    public int getConsecutiveSuccesses() {
        return consecutiveSuccesses;
    }

    public void setConsecutiveSuccesses(int consecutiveSuccesses) {
        this.consecutiveSuccesses = consecutiveSuccesses;
    }

    public LocalDateTime getLastSuccessfulCheckAt() {
        return lastSuccessfulCheckAt;
    }

    public void setLastSuccessfulCheckAt(LocalDateTime lastSuccessfulCheckAt) {
        this.lastSuccessfulCheckAt = lastSuccessfulCheckAt;
    }

    public LocalDateTime getLastFailedCheckAt() {
        return lastFailedCheckAt;
    }

    public void setLastFailedCheckAt(LocalDateTime lastFailedCheckAt) {
        this.lastFailedCheckAt = lastFailedCheckAt;
    }

    public String getLastFailureReason() {
        return lastFailureReason;
    }

    public void setLastFailureReason(String lastFailureReason) {
        this.lastFailureReason = lastFailureReason;
    }

    public Integer getLastHttpStatusCode() {
        return lastHttpStatusCode;
    }

    public void setLastHttpStatusCode(Integer lastHttpStatusCode) {
        this.lastHttpStatusCode = lastHttpStatusCode;
    }

    public Long getLastResponseTime() {
        return lastResponseTime;
    }

    public void setLastResponseTime(Long lastResponseTime) {
        this.lastResponseTime = lastResponseTime;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
