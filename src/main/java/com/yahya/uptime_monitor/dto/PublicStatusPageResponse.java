package com.yahya.uptime_monitor.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PublicStatusPageResponse(
        LocalDateTime generatedAt,
        List<PublicWebsiteStatusResponse> websites
) {
}
