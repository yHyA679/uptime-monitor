package com.yahya.uptime_monitor.controller;

import com.yahya.uptime_monitor.dto.PageResponse;
import com.yahya.uptime_monitor.dto.WebsiteSettingsRequest;
import com.yahya.uptime_monitor.dto.WebsiteStats;
import com.yahya.uptime_monitor.model.Website;
import com.yahya.uptime_monitor.service.WebsiteService;
import com.yahya.uptime_monitor.service.WebsiteCheckCoordinator;
import com.yahya.uptime_monitor.util.PaginationSupport;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/websites")
public class WebsiteController {

    private final WebsiteService websiteService;
    private final WebsiteCheckCoordinator websiteCheckCoordinator;

    public WebsiteController(
            WebsiteService websiteService,
            WebsiteCheckCoordinator websiteCheckCoordinator
    ) {
        this.websiteService = websiteService;
        this.websiteCheckCoordinator = websiteCheckCoordinator;
    }

    @PostMapping
    public Website addWebsite(@Valid @RequestBody Website website) {
        return websiteService.addWebsite(website);
    }

    @GetMapping
    public Object getAllWebsites(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @PageableDefault(size = PaginationSupport.DEFAULT_PAGE_SIZE) Pageable pageable
    ) {
        PaginationSupport.validate(page, size);
        String effectiveSortBy = sortBy;
        String effectiveSortDirection = sortDirection;

        if (effectiveSortBy == null && pageable.getSort().isSorted()) {
            Sort.Order requestedSort = pageable.getSort().iterator().next();
            effectiveSortBy = requestedSort.getProperty();
            if (effectiveSortDirection == null) {
                effectiveSortDirection = requestedSort.getDirection().name();
            }
        }

        boolean queryRequested = hasWebsiteQuery(
                status,
                enabled,
                tag,
                name,
                effectiveSortBy,
                effectiveSortDirection
        );

        if (PaginationSupport.isRequested(page, size)) {
            if (queryRequested) {
                return PageResponse.from(websiteService.findWebsites(
                        status,
                        enabled,
                        tag,
                        name,
                        effectiveSortBy,
                        effectiveSortDirection,
                        pageable
                ));
            }
            return PageResponse.from(websiteService.getAllWebsites(pageable));
        }

        if (queryRequested) {
            return websiteService.findWebsites(
                    status,
                    enabled,
                    tag,
                    name,
                    effectiveSortBy,
                    effectiveSortDirection
            );
        }

        return websiteService.getAllWebsites();
    }

    private boolean hasWebsiteQuery(
            String status,
            Boolean enabled,
            String tag,
            String name,
            String sortBy,
            String sortDirection
    ) {
        return status != null
                || enabled != null
                || tag != null
                || name != null
                || sortBy != null
                || sortDirection != null;
    }

    @DeleteMapping("/{id}")
    public void deleteWebsite(@PathVariable Long id) {
        websiteService.deleteWebsite(id);
    }

    @GetMapping("/{id}")
    public Website getWebsite(@PathVariable Long id) {
        return websiteService.getWebsite(id);
    }

    @PostMapping("/{id}/check")
    public Website checkWebsite(@PathVariable Long id) {
        return websiteCheckCoordinator.checkNow(id);
    }

    @GetMapping("/{id}/history")
    public Object getHistory(
            @PathVariable Long id,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @PageableDefault(size = PaginationSupport.DEFAULT_PAGE_SIZE) Pageable pageable
    ) {
        PaginationSupport.validate(page, size);
        if (PaginationSupport.isRequested(page, size)) {
            return PageResponse.from(websiteService.getHistory(id, pageable));
        }
        return websiteService.getHistory(id);
    }

    @GetMapping("/{id}/incidents")
    public Object getIncidents(
            @PathVariable Long id,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @PageableDefault(size = PaginationSupport.DEFAULT_PAGE_SIZE) Pageable pageable
    ) {
        PaginationSupport.validate(page, size);
        if (PaginationSupport.isRequested(page, size)) {
            return PageResponse.from(websiteService.getIncidents(id, pageable));
        }
        return websiteService.getIncidents(id);
    }

    @GetMapping("/{id}/stats")
    public WebsiteStats getStats(@PathVariable Long id) {
        return websiteService.getStats(id);
    }

    @PatchMapping("/{id}")
    public Website updateMonitoringSettings(
            @PathVariable Long id,
            @Valid @RequestBody WebsiteSettingsRequest settings
    ) {
        return websiteService.updateMonitoringSettings(id, settings);
    }
}
