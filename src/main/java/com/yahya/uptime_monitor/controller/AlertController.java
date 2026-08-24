package com.yahya.uptime_monitor.controller;

import com.yahya.uptime_monitor.dto.PageResponse;
import com.yahya.uptime_monitor.service.AlertService;
import com.yahya.uptime_monitor.util.PaginationSupport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/alerts")
    public Object getAllAlerts(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @PageableDefault(size = PaginationSupport.DEFAULT_PAGE_SIZE) Pageable pageable
    ) {
        PaginationSupport.validate(page, size);
        if (PaginationSupport.isRequested(page, size)) {
            return PageResponse.from(alertService.getAllAlerts(pageable));
        }
        return alertService.getAllAlerts();
    }

    @GetMapping("/websites/{websiteId}/alerts")
    public Object getAlertsForWebsite(
            @PathVariable Long websiteId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @PageableDefault(size = PaginationSupport.DEFAULT_PAGE_SIZE) Pageable pageable
    ) {
        PaginationSupport.validate(page, size);
        if (PaginationSupport.isRequested(page, size)) {
            return PageResponse.from(alertService.getAlertsForWebsite(websiteId, pageable));
        }
        return alertService.getAlertsForWebsite(websiteId);
    }
}
