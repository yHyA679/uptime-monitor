package com.yahya.uptime_monitor.service;

import com.yahya.uptime_monitor.dto.WebsiteSettingsRequest;
import com.yahya.uptime_monitor.dto.WebsiteStats;
import com.yahya.uptime_monitor.config.MonitoringExecutionProperties;
import com.yahya.uptime_monitor.model.Incident;
import com.yahya.uptime_monitor.model.MonitoringResult;
import com.yahya.uptime_monitor.model.Website;
import com.yahya.uptime_monitor.repository.IncidentRepository;
import com.yahya.uptime_monitor.repository.MonitoringResultRepository;
import com.yahya.uptime_monitor.repository.WebsiteMetricsProjection;
import com.yahya.uptime_monitor.repository.WebsiteRepository;
import com.yahya.uptime_monitor.repository.WebsiteStatsProjection;
import com.yahya.uptime_monitor.util.PaginationSupport;
import com.yahya.uptime_monitor.util.WebsiteUrlSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.server.ResponseStatusException;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.function.Function;

@Service
public class WebsiteService {

    private static final Logger log = LoggerFactory.getLogger(WebsiteService.class);
    private final WebsiteRepository websiteRepository;
    private final MonitoringResultRepository monitoringResultRepository;
    private final IncidentRepository incidentRepository;
    private final AlertService alertService;
    private final MonitoringExecutionProperties monitoringProperties;
    private final TransactionOperations transactions;

    public WebsiteService(
            WebsiteRepository websiteRepository,
            MonitoringResultRepository monitoringResultRepository,
            IncidentRepository incidentRepository,
            AlertService alertService,
            MonitoringExecutionProperties monitoringProperties,
            TransactionOperations transactions
    ) {
        this.websiteRepository = websiteRepository;
        this.monitoringResultRepository = monitoringResultRepository;
        this.incidentRepository = incidentRepository;
        this.alertService = alertService;
        this.monitoringProperties = monitoringProperties;
        this.transactions = transactions;
    }

    @Transactional
    public Website addWebsite(Website website) {
        website.setName(website.getName().trim());
        website.setTag(normalize(website.getTag()));

        try {
            website.setUrl(WebsiteUrlSupport.normalize(website.getUrl()));
        } catch (IllegalArgumentException exception) {
            throw badRequest(exception.getMessage());
        }

        if (websiteRepository.existsByUrl(website.getUrl())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A website with this URL already exists"
            );
        }

        clearMonitoringState(website);

        return websiteRepository.save(website);
    }

    public List<Website> getAllWebsites() {
        return websiteRepository.findAll();
    }

    public Website getWebsite(Long id) {
        return websiteRepository.findById(id)
                .orElseThrow(() -> websiteNotFound(id));
    }

    public Page<Website> getAllWebsites(Pageable pageable) {
        Sort sort = Sort.by(
                Sort.Order.asc("name"),
                Sort.Order.asc("id")
        );
        return websiteRepository.findAll(PaginationSupport.withSort(pageable, sort));
    }

    @Transactional(readOnly = true)
    public List<Website> findWebsites(
            String status,
            Boolean enabled,
            String tag,
            String name,
            String sortBy,
            String sortDirection
    ) {
        WebsiteQuery query = websiteQuery(status, enabled, tag, name, sortBy, sortDirection);
        return findAndSortWebsites(query);
    }

    @Transactional(readOnly = true)
    public Page<Website> findWebsites(
            String status,
            Boolean enabled,
            String tag,
            String name,
            String sortBy,
            String sortDirection,
            Pageable pageable
    ) {
        List<Website> websites = findWebsites(
                status,
                enabled,
                tag,
                name,
                sortBy,
                sortDirection
        );
        int start = Math.min((int) pageable.getOffset(), websites.size());
        int end = Math.min(start + pageable.getPageSize(), websites.size());

        return new PageImpl<>(
                List.copyOf(websites.subList(start, end)),
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()),
                websites.size()
        );
    }

    @Transactional
    public void deleteWebsite(Long id) {
        if (!websiteRepository.existsById(id)) {
            throw websiteNotFound(id);
        }

        monitoringResultRepository.deleteByWebsiteId(id);
        incidentRepository.deleteByWebsiteId(id);
        alertService.deleteAlertsForWebsite(id);
        websiteRepository.deleteById(id);
    }

    public Website checkWebsite(Long id) {
        Website websiteSnapshot = websiteRepository.findById(id)
                .orElseThrow(() -> websiteNotFound(id));

        log.atDebug()
                .addKeyValue("websiteId", websiteSnapshot.getId())
                .addKeyValue("websiteName", websiteSnapshot.getName())
                .log("Website check started");

        long startTime = System.nanoTime();
        CheckOutcome outcome = performCheck(websiteSnapshot.getUrl());
        long responseTime = (System.nanoTime() - startTime) / 1_000_000;
        LocalDateTime checkedAt = LocalDateTime.now();

        return Objects.requireNonNull(transactions.execute(transactionStatus -> {
            Website website = websiteRepository.findById(id)
                    .orElseThrow(() -> websiteNotFound(id));
            return saveCheckOutcome(website, outcome, responseTime, checkedAt);
        }));
    }

    public List<MonitoringResult> getHistory(Long id) {
        if (!websiteRepository.existsById(id)) {
            throw websiteNotFound(id);
        }

        return monitoringResultRepository.findByWebsiteIdOrderByCheckedAtDesc(id);
    }

    public Page<MonitoringResult> getHistory(Long id, Pageable pageable) {
        if (!websiteRepository.existsById(id)) {
            throw websiteNotFound(id);
        }

        Sort sort = Sort.by(
                Sort.Order.desc("checkedAt"),
                Sort.Order.desc("id")
        );
        return monitoringResultRepository.findByWebsiteId(
                id,
                PaginationSupport.withSort(pageable, sort)
        );
    }

    public List<Incident> getIncidents(Long id) {
        if (!websiteRepository.existsById(id)) {
            throw websiteNotFound(id);
        }

        return incidentRepository.findByWebsiteIdOrderByStartedAtDesc(id);
    }

    public Page<Incident> getIncidents(Long id, Pageable pageable) {
        if (!websiteRepository.existsById(id)) {
            throw websiteNotFound(id);
        }

        Sort sort = Sort.by(
                Sort.Order.desc("startedAt"),
                Sort.Order.desc("id")
        );
        return incidentRepository.findByWebsiteId(
                id,
                PaginationSupport.withSort(pageable, sort)
        );
    }

    public WebsiteStats getStats(Long id) {
        if (!websiteRepository.existsById(id)) {
            throw websiteNotFound(id);
        }

        WebsiteStatsProjection stats = monitoringResultRepository.findStatsByWebsiteId(id);
        long totalChecks = valueOrZero(stats.getTotalChecks());
        long upChecks = valueOrZero(stats.getUpChecks());
        long downChecks = valueOrZero(stats.getDownChecks());
        double uptimePercentage = totalChecks == 0
                ? 0.0
                : upChecks * 100.0 / totalChecks;
        long averageResponseTime = Math.round(
                stats.getAverageResponseTime() == null
                        ? 0.0 : stats.getAverageResponseTime()
        );

        return new WebsiteStats(
                id,
                totalChecks,
                upChecks,
                downChecks,
                uptimePercentage,
                averageResponseTime,
                stats.getLastCheckedAt()
        );
    }

    private static long valueOrZero(Long value) {
        return value == null ? 0 : value;
    }

    private static void clearMonitoringState(Website website) {
        website.setStatus(null);
        website.setConsecutiveFailures(0);
        website.setConsecutiveSuccesses(0);
        website.setLastSuccessfulCheckAt(null);
        website.setLastFailedCheckAt(null);
        website.setLastFailureReason(null);
        website.setLastHttpStatusCode(null);
        website.setLastResponseTime(null);
    }

    @Transactional
    public Website updateMonitoringSettings(Long id, WebsiteSettingsRequest settings) {
        Website website = websiteRepository.findById(id)
                .orElseThrow(() -> websiteNotFound(id));

        if (settings.getCheckIntervalSeconds() != null) {
            website.setCheckIntervalSeconds(settings.getCheckIntervalSeconds());
        }

        if (settings.getEnabled() != null) {
            website.setEnabled(settings.getEnabled());
        }

        if (settings.getPubliclyVisible() != null) {
            website.setPubliclyVisible(settings.getPubliclyVisible());
        }

        if (settings.getTag() != null) {
            website.setTag(normalize(settings.getTag()));
        }

        if (settings.getFailureThreshold() != null) {
            website.setFailureThreshold(settings.getFailureThreshold());
        }

        if (settings.getRecoveryThreshold() != null) {
            website.setRecoveryThreshold(settings.getRecoveryThreshold());
        }

        return websiteRepository.save(website);
    }

    private List<Website> findAndSortWebsites(WebsiteQuery query) {
        List<Website> websites = websiteRepository.findFiltered(
                query.status(),
                query.enabled(),
                query.tag(),
                query.name()
        );
        Map<Long, WebsiteMetrics> metrics = loadMetrics(websites, query.sortField());

        return websites.stream()
                .sorted(websiteComparator(query.sortField(), query.ascending(), metrics))
                .toList();
    }

    private Map<Long, WebsiteMetrics> loadMetrics(
            List<Website> websites,
            WebsiteSortField sortField
    ) {
        if (!sortField.requiresMetrics() || websites.isEmpty()) {
            return Map.of();
        }

        List<Long> websiteIds = websites.stream().map(Website::getId).toList();
        Map<Long, WebsiteMetrics> metrics = new HashMap<>();

        for (WebsiteMetricsProjection projection
                : monitoringResultRepository.findMetricsByWebsiteIds(websiteIds)) {
            metrics.put(projection.getWebsiteId(), new WebsiteMetrics(
                    projection.getUptimePercentage() == null
                            ? 0.0 : projection.getUptimePercentage(),
                    projection.getAverageResponseTime() == null
                            ? 0.0 : projection.getAverageResponseTime(),
                    projection.getLastCheckedAt()
            ));
        }

        return metrics;
    }

    private Comparator<Website> websiteComparator(
            WebsiteSortField sortField,
            boolean ascending,
            Map<Long, WebsiteMetrics> metrics
    ) {
        Comparator<Website> primary = switch (sortField) {
            case NAME -> comparing(Website::getName, ascending);
            case STATUS -> comparing(
                    website -> website.getStatus() == null ? "PENDING" : website.getStatus(),
                    ascending
            );
            case UPTIME_PERCENTAGE -> comparing(
                    website -> metrics.getOrDefault(website.getId(), WebsiteMetrics.EMPTY)
                            .uptimePercentage(),
                    ascending
            );
            case AVERAGE_RESPONSE_TIME -> comparing(
                    website -> metrics.getOrDefault(website.getId(), WebsiteMetrics.EMPTY)
                            .averageResponseTime(),
                    ascending
            );
            case LAST_CHECKED -> comparing(
                    website -> metrics.getOrDefault(website.getId(), WebsiteMetrics.EMPTY)
                            .lastCheckedAt(),
                    ascending
            );
        };

        Comparator<Website> stableTieBreaker = comparing(Website::getName, true)
                .thenComparing(comparing(Website::getId, true));
        return primary.thenComparing(stableTieBreaker);
    }

    private <T extends Comparable<? super T>> Comparator<Website> comparing(
            Function<Website, T> value,
            boolean ascending
    ) {
        Comparator<T> valueComparator = ascending
                ? Comparator.nullsLast(Comparator.naturalOrder())
                : Comparator.nullsLast(Comparator.reverseOrder());
        return Comparator.comparing(value, valueComparator);
    }

    private WebsiteQuery websiteQuery(
            String status,
            Boolean enabled,
            String tag,
            String name,
            String sortBy,
            String sortDirection
    ) {
        String normalizedStatus = normalize(status);
        if (normalizedStatus != null) {
            normalizedStatus = normalizedStatus.toUpperCase(Locale.ROOT);
            if (!List.of("UP", "DOWN", "PENDING").contains(normalizedStatus)) {
                throw badRequest("Status must be UP, DOWN, or PENDING");
            }
        }

        WebsiteSortField sortField = WebsiteSortField.from(sortBy);
        String direction = normalize(sortDirection);
        if (direction != null
                && !"asc".equalsIgnoreCase(direction)
                && !"desc".equalsIgnoreCase(direction)) {
            throw badRequest("Sort direction must be asc or desc");
        }

        return new WebsiteQuery(
                normalizedStatus,
                enabled,
                normalize(tag),
                normalize(name),
                sortField,
                direction == null || "asc".equalsIgnoreCase(direction)
        );
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private Website saveCheckOutcome(
            Website website,
            CheckOutcome outcome,
            long responseTime,
            LocalDateTime checkedAt
    ) {
        String previousStatus = website.getStatus();

        applyCheckOutcome(website, outcome, responseTime, checkedAt);
        Website savedWebsite = websiteRepository.save(website);

        MonitoringResult result = new MonitoringResult();
        result.setWebsite(savedWebsite);
        result.setStatus(outcome.up() ? "UP" : "DOWN");
        result.setResponseTime(responseTime);
        result.setHttpStatusCode(outcome.httpStatusCode());
        result.setFailureReason(outcome.failureReason());
        result.setCheckedAt(checkedAt);

        monitoringResultRepository.save(result);

        logCheckResult(savedWebsite, previousStatus, outcome, responseTime);
        handleStatusTransition(savedWebsite, previousStatus, savedWebsite.getStatus(), checkedAt);

        return savedWebsite;
    }

    private void logCheckResult(
            Website website,
            String previousStatus,
            CheckOutcome outcome,
            long responseTime
    ) {
        var event = outcome.up() ? log.atInfo() : log.atWarn();
        event.addKeyValue("websiteId", website.getId())
                .addKeyValue("websiteName", website.getName())
                .addKeyValue("observedStatus", outcome.up() ? "UP" : "DOWN")
                .addKeyValue("previousStatus", previousStatus)
                .addKeyValue("currentStatus", website.getStatus())
                .addKeyValue("responseTimeMs", responseTime)
                .addKeyValue("httpStatusCode", outcome.httpStatusCode());

        if (!outcome.up()) {
            event.addKeyValue("failureCategory", failureCategory(outcome));
        }

        event.log("Website check completed");
    }

    private String failureCategory(CheckOutcome outcome) {
        if (outcome.httpStatusCode() != null) {
            return "HTTP_" + outcome.httpStatusCode();
        }

        String reason = outcome.failureReason();
        if (reason == null || reason.isBlank()) {
            return "CHECK_FAILED";
        }

        int separator = reason.indexOf(':');
        return separator < 0 ? reason : reason.substring(0, separator);
    }

    private CheckOutcome performCheck(String websiteUrl) {
        HttpURLConnection connection = null;

        try {
            WebsiteUrlSupport.validateMonitoringTarget(
                    websiteUrl,
                    monitoringProperties.isAllowPrivateTargets()
            );
            URL url = new URL(websiteUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(monitoringProperties.getConnectTimeoutMs());
            connection.setReadTimeout(monitoringProperties.getReadTimeoutMs());
            connection.setRequestProperty("User-Agent", "uptime-monitor/1.0");

            int responseCode = connection.getResponseCode();
            boolean up = responseCode >= 200 && responseCode < 400;
            String failureReason = up
                    ? null
                    : httpFailureReason(responseCode, connection.getResponseMessage());

            return new CheckOutcome(up, responseCode, failureReason);
        } catch (Exception exception) {
            return new CheckOutcome(false, null, classifyFailure(exception));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void applyCheckOutcome(
            Website website,
            CheckOutcome outcome,
            long responseTime,
            LocalDateTime checkedAt
    ) {
        website.setLastResponseTime(responseTime);
        website.setLastHttpStatusCode(outcome.httpStatusCode());

        if (outcome.up()) {
            website.setConsecutiveSuccesses(increment(website.getConsecutiveSuccesses()));
            website.setConsecutiveFailures(0);
            website.setLastSuccessfulCheckAt(checkedAt);

            if (website.getStatus() == null
                    || ("DOWN".equals(website.getStatus())
                    && website.getConsecutiveSuccesses() >= website.getRecoveryThreshold())) {
                website.setStatus("UP");
            }
        } else {
            website.setConsecutiveFailures(increment(website.getConsecutiveFailures()));
            website.setConsecutiveSuccesses(0);
            website.setLastFailedCheckAt(checkedAt);
            website.setLastFailureReason(outcome.failureReason());

            if (website.getConsecutiveFailures() >= website.getFailureThreshold()) {
                website.setStatus("DOWN");
            }
        }
    }

    static String classifyFailure(Exception exception) {
        String category;

        if (exception instanceof SecurityException) {
            category = "UNSAFE_TARGET";
        } else if (exception instanceof UnknownHostException) {
            category = "DNS_RESOLUTION_FAILED";
        } else if (exception instanceof SocketTimeoutException) {
            category = "TIMEOUT";
        } else if (exception instanceof ConnectException
                && exception.getMessage() != null
                && exception.getMessage().toLowerCase().contains("refused")) {
            category = "CONNECTION_REFUSED";
        } else if (exception instanceof ConnectException) {
            category = "CONNECTION_FAILED";
        } else if (exception instanceof SSLException) {
            category = "TLS_ERROR";
        } else if (exception instanceof MalformedURLException) {
            category = "INVALID_URL";
        } else if (exception instanceof IOException) {
            category = "NETWORK_ERROR";
        } else {
            category = "CHECK_FAILED";
        }

        String message = exception.getMessage();
        return limited(message == null || message.isBlank()
                ? category
                : category + ": " + message);
    }

    private static int increment(int value) {
        return value == Integer.MAX_VALUE ? value : value + 1;
    }

    private static String httpFailureReason(int responseCode, String responseMessage) {
        return limited(responseMessage == null || responseMessage.isBlank()
                ? "HTTP " + responseCode
                : "HTTP " + responseCode + " " + responseMessage);
    }

    private static String limited(String value) {
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }

    private void handleStatusTransition(
            Website website,
            String previousStatus,
            String currentStatus,
            LocalDateTime checkedAt
    ) {
        if ("UP".equals(previousStatus) && "DOWN".equals(currentStatus)) {
            if (findOpenIncident(website.getId()).isEmpty()) {
                Incident incident = new Incident();
                incident.setWebsite(website);
                incident.setStartedAt(checkedAt);
                incident.setStatus("OPEN");

                incidentRepository.save(incident);

                log.atWarn()
                        .addKeyValue("websiteId", website.getId())
                        .addKeyValue("websiteName", website.getName())
                        .addKeyValue("startedAt", checkedAt)
                        .log("Incident opened");
            }
            alertService.recordDowntime(website, checkedAt);
        } else if ("DOWN".equals(previousStatus) && "UP".equals(currentStatus)) {
            findOpenIncident(website.getId()).ifPresent(incident -> {
                incident.setResolvedAt(checkedAt);
                incident.setDurationSeconds(
                        Duration.between(incident.getStartedAt(), checkedAt).getSeconds()
                );
                incident.setStatus("RESOLVED");

                incidentRepository.save(incident);

                log.atInfo()
                        .addKeyValue("websiteId", website.getId())
                        .addKeyValue("websiteName", website.getName())
                        .addKeyValue("incidentId", incident.getId())
                        .addKeyValue("durationSeconds", incident.getDurationSeconds())
                        .addKeyValue("resolvedAt", checkedAt)
                        .log("Incident resolved");
            });
            alertService.recordRecovery(website, checkedAt);
        }
    }

    private Optional<Incident> findOpenIncident(Long websiteId) {
        return incidentRepository
                .findFirstByWebsiteIdAndStatusOrderByStartedAtDesc(websiteId, "OPEN");
    }

    private ResponseStatusException websiteNotFound(Long id) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Website with id " + id + " was not found"
        );
    }

    private enum WebsiteSortField {
        NAME(false),
        STATUS(false),
        UPTIME_PERCENTAGE(true),
        AVERAGE_RESPONSE_TIME(true),
        LAST_CHECKED(true);

        private final boolean requiresMetrics;

        WebsiteSortField(boolean requiresMetrics) {
            this.requiresMetrics = requiresMetrics;
        }

        boolean requiresMetrics() {
            return requiresMetrics;
        }

        static WebsiteSortField from(String value) {
            String normalized = normalize(value);
            if (normalized == null) {
                return NAME;
            }

            normalized = normalized
                    .replace("_", "")
                    .replace("-", "")
                    .toLowerCase(Locale.ROOT);

            return switch (normalized) {
                case "name" -> NAME;
                case "status" -> STATUS;
                case "uptimepercentage" -> UPTIME_PERCENTAGE;
                case "averageresponsetime" -> AVERAGE_RESPONSE_TIME;
                case "lastchecked" -> LAST_CHECKED;
                default -> throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sort field must be name, status, uptimePercentage, "
                                + "averageResponseTime, or lastChecked"
                );
            };
        }
    }

    private record WebsiteQuery(
            String status,
            Boolean enabled,
            String tag,
            String name,
            WebsiteSortField sortField,
            boolean ascending
    ) {
    }

    private record WebsiteMetrics(
            double uptimePercentage,
            double averageResponseTime,
            LocalDateTime lastCheckedAt
    ) {
        private static final WebsiteMetrics EMPTY = new WebsiteMetrics(0.0, 0.0, null);
    }

    private record CheckOutcome(
            boolean up,
            Integer httpStatusCode,
            String failureReason
    ) {
    }
}
