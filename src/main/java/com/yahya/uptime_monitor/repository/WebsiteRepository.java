package com.yahya.uptime_monitor.repository;

import com.yahya.uptime_monitor.model.Website;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WebsiteRepository extends JpaRepository<Website, Long> {

    boolean existsByUrl(String url);

    long countByEnabledTrueAndStatus(String status);

    long countByEnabledFalse();

    List<Website> findByEnabledTrue();

    List<Website> findByPubliclyVisibleTrueOrderByNameAsc();

    @Query("""
            select website
            from Website website
            where (:status is null
                    or upper(coalesce(website.status, 'PENDING')) = :status)
              and (:enabled is null or website.enabled = :enabled)
              and (:tag is null or lower(website.tag) = lower(:tag))
              and (:name is null
                    or lower(website.name) like lower(concat('%', :name, '%')))
            """)
    List<Website> findFiltered(
            @Param("status") String status,
            @Param("enabled") Boolean enabled,
            @Param("tag") String tag,
            @Param("name") String name
    );
}
