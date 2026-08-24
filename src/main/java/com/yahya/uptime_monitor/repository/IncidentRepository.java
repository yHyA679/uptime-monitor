package com.yahya.uptime_monitor.repository;

import com.yahya.uptime_monitor.model.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    long countByStatus(String status);

    long countByStartedAtGreaterThanEqual(LocalDateTime cutoff);

    List<Incident> findByWebsiteIdOrderByStartedAtDesc(Long websiteId);

    Page<Incident> findByWebsiteId(Long websiteId, Pageable pageable);

    List<Incident> findTop5ByWebsiteIdOrderByStartedAtDesc(Long websiteId);

    Optional<Incident> findFirstByWebsiteIdAndStatusOrderByStartedAtDesc(
            Long websiteId,
            String status
    );

    void deleteByWebsiteId(Long websiteId);
}
