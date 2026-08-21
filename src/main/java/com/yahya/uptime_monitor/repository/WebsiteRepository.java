package com.yahya.uptime_monitor.repository;

import com.yahya.uptime_monitor.model.Website;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebsiteRepository extends JpaRepository<Website, Long> {
}