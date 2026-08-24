package com.yahya.uptime_monitor.repository;

import com.yahya.uptime_monitor.model.MonitoringResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MonitoringResultRepository
        extends JpaRepository<MonitoringResult, Long> {

    List<MonitoringResult> findByWebsiteIdOrderByCheckedAtDesc(Long websiteId);

    Page<MonitoringResult> findByWebsiteId(Long websiteId, Pageable pageable);

    @Query("""
            select count(result) as totalChecks,
                   sum(case when result.status = 'UP' then 1 else 0 end) as upChecks,
                   sum(case when result.status = 'DOWN' then 1 else 0 end) as downChecks,
                   coalesce(avg(result.responseTime), 0.0) as averageResponseTime,
                   max(result.checkedAt) as lastCheckedAt
            from MonitoringResult result
            where result.website.id = :websiteId
            """)
    WebsiteStatsProjection findStatsByWebsiteId(@Param("websiteId") Long websiteId);

    long countByCheckedAtGreaterThanEqual(LocalDateTime cutoff);

    @Query("""
            select case when count(result) = 0 then 0.0
                        else (100.0 * sum(case when result.status = 'UP' then 1 else 0 end)
                            / count(result)) end as averageUptime,
                   coalesce(avg(result.responseTime), 0.0) as averageResponseTime
            from MonitoringResult result
            """)
    DashboardMetricsProjection findDashboardMetrics();

    @Query("""
            select result.website.id as websiteId,
                   (100.0 * sum(case when result.status = 'UP' then 1 else 0 end)
                       / count(result)) as uptimePercentage,
                   avg(result.responseTime) as averageResponseTime,
                   max(result.checkedAt) as lastCheckedAt
            from MonitoringResult result
            where result.website.id in :websiteIds
            group by result.website.id
            """)
    List<WebsiteMetricsProjection> findMetricsByWebsiteIds(
            @Param("websiteIds") List<Long> websiteIds
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from MonitoringResult result where result.checkedAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);

    void deleteByWebsiteId(Long websiteId);
}
