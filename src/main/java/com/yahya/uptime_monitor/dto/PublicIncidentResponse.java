package com.yahya.uptime_monitor.dto;

import java.time.LocalDateTime;

public record PublicIncidentResponse(
        LocalDateTime startedAt,
        LocalDateTime resolvedAt,
        Long durationSeconds,
        String status
) {
}
