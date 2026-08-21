package com.yahya.uptime_monitor.controller;

import com.yahya.uptime_monitor.model.Website;
import com.yahya.uptime_monitor.service.WebsiteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/websites")
public class WebsiteController {

    private final WebsiteService websiteService;

    public WebsiteController(WebsiteService websiteService) {
        this.websiteService = websiteService;
    }

    @PostMapping
    public Website addWebsite(@RequestBody Website website) {
        return websiteService.addWebsite(website);
    }

    @GetMapping
    public List<Website> getAllWebsites() {
        return websiteService.getAllWebsites();
    }

    @DeleteMapping("/{id}")
    public void deleteWebsite(@PathVariable Long id) {
        websiteService.deleteWebsite(id);
    }
}