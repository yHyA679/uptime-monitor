package com.yahya.uptime_monitor.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "monitoring_results",
        indexes = {
                @Index(
                        name = "idx_monitoring_results_checked_at",
                        columnList = "checked_at"
                ),
                @Index(
                        name = "idx_monitoring_results_website_checked_at",
                        columnList = "website_id, checked_at"
                )
        }
)
public class MonitoringResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String status;

    private long responseTime;

    private Integer httpStatusCode;

    @Column(length = 1000)
    private String failureReason;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    @ManyToOne
    @JoinColumn(name = "website_id", nullable = false)
    private Website website;

    public MonitoringResult() {
    }

    public Long getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }

    public Website getWebsite() {
        return website;
    }

    public void setWebsite(Website website) {
        this.website = website;
    }
}
