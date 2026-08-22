package com.yahya.uptime_monitor.service;

import com.yahya.uptime_monitor.model.Website;
import com.yahya.uptime_monitor.repository.WebsiteRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
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

    public Website checkWebsite(Long id) {
        Website website = websiteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Website not found"));

        checkAndUpdateWebsite(website);

        return websiteRepository.save(website);
    }

    @Scheduled(fixedRate = 60000)
    public void checkAllWebsites() {
        System.out.println("Running automatic website check...");

        List<Website> websites = websiteRepository.findAll();

        for (Website website : websites) {
            checkAndUpdateWebsite(website);
            websiteRepository.save(website);
        }
    }

    private void checkAndUpdateWebsite(Website website) {
        try {
            URL url = new URL(website.getUrl());

            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();

            if (responseCode >= 200 && responseCode < 400) {
                website.setStatus("UP");
            } else {
                website.setStatus("DOWN");
            }

            connection.disconnect();

        } catch (Exception e) {
            website.setStatus("DOWN");
        }
    }
}