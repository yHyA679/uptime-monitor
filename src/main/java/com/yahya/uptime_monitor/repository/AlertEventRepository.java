package com.yahya.uptime_monitor.repository;

import com.yahya.uptime_monitor.model.AlertEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertEventRepository extends JpaRepository<AlertEvent, Long> {

    List<AlertEvent> findAllByOrderByCreatedAtDesc();

    Page<AlertEvent> findAll(Pageable pageable);

    List<AlertEvent> findByWebsiteIdOrderByCreatedAtDesc(Long websiteId);

    Page<AlertEvent> findByWebsiteId(Long websiteId, Pageable pageable);

    void deleteByWebsiteId(Long websiteId);
}
