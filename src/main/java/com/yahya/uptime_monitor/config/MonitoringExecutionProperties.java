package com.yahya.uptime_monitor.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "monitoring")
public class MonitoringExecutionProperties {

    @Min(1)
    private int workerThreads = 4;

    @Min(1)
    private int queueCapacity = 100;

    @Min(1000)
    private long schedulerDelayMs = 10_000;

    @Min(1)
    private int retentionDays = 30;

    @Min(60_000)
    private long retentionCleanupDelayMs = 86_400_000;

    private boolean allowPrivateTargets = false;

    @Min(100)
    @Max(60_000)
    private int connectTimeoutMs = 5_000;

    @Min(100)
    @Max(60_000)
    private int readTimeoutMs = 5_000;

    public int getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public long getSchedulerDelayMs() {
        return schedulerDelayMs;
    }

    public void setSchedulerDelayMs(long schedulerDelayMs) {
        this.schedulerDelayMs = schedulerDelayMs;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public long getRetentionCleanupDelayMs() {
        return retentionCleanupDelayMs;
    }

    public void setRetentionCleanupDelayMs(long retentionCleanupDelayMs) {
        this.retentionCleanupDelayMs = retentionCleanupDelayMs;
    }

    public boolean isAllowPrivateTargets() {
        return allowPrivateTargets;
    }

    public void setAllowPrivateTargets(boolean allowPrivateTargets) {
        this.allowPrivateTargets = allowPrivateTargets;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}
