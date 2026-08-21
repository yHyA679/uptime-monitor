package com.yahya.uptime_monitor.service;

import com.yahya.uptime_monitor.model.Website;
import com.yahya.uptime_monitor.repository.WebsiteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WebsiteService {

    private final WebsiteRepository websiteRepository;

    public WebsiteService(WebsiteRepository websiteRepository) {
        this.websiteRepository = websiteRepository;
    }

    public Website addWebsite(Website website) {
        return websiteRepository.save(website);
    }

    public List<Website> getAllWebsites() {
        return websiteRepository.findAll();
    }

    public void deleteWebsite(Long id) {
        websiteRepository.deleteById(id);
    }
}