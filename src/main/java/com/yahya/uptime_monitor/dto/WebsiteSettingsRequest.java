package com.yahya.uptime_monitor.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class WebsiteSettingsRequest {

    @Min(10)
    @Max(3600)
    private Integer checkIntervalSeconds;

    private Boolean enabled;

    private Boolean publiclyVisible;

    @Size(max = 50)
    private String tag;

    @Min(1)
    @Max(10)
    private Integer failureThreshold;

    @Min(1)
    @Max(10)
    private Integer recoveryThreshold;

    public Integer getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    public void setCheckIntervalSeconds(Integer checkIntervalSeconds) {
        this.checkIntervalSeconds = checkIntervalSeconds;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getPubliclyVisible() {
        return publiclyVisible;
    }

    public void setPubliclyVisible(Boolean publiclyVisible) {
        this.publiclyVisible = publiclyVisible;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Integer getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(Integer failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public Integer getRecoveryThreshold() {
        return recoveryThreshold;
    }

    public void setRecoveryThreshold(Integer recoveryThreshold) {
        this.recoveryThreshold = recoveryThreshold;
    }
}
