package com.yahya.uptime_monitor.controller;

import com.yahya.uptime_monitor.dto.PublicStatusPageResponse;
import com.yahya.uptime_monitor.service.PublicStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicStatusController {

    private final PublicStatusService publicStatusService;

    public PublicStatusController(PublicStatusService publicStatusService) {
        this.publicStatusService = publicStatusService;
    }

    @GetMapping("/status")
    public PublicStatusPageResponse getPublicStatus() {
        return publicStatusService.getPublicStatus();
    }
}
