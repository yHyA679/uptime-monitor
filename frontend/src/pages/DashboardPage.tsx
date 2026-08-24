import {
  ArrowRight,
  CheckCircle,
  ClockCountdown,
  GlobeHemisphereWest,
  Pulse,
  WarningCircle,
} from "@phosphor-icons/react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { websiteApi } from "../api/client";
import { AddWebsiteDialog } from "../components/AddWebsiteDialog";
import { ErrorState, LoadingState } from "../components/AsyncStates";
import { IncidentList } from "../components/IncidentList";
import { MetricCard } from "../components/MetricCard";
import { ResponseTimeChart } from "../components/ResponseTimeChart";
import { StatusBadge } from "../components/StatusBadge";
import { formatPercentage, formatRelativeTime, formatResponseTime, hostname } from "../lib/format";
import type { Incident, MonitoringResult, WebsiteOverview } from "../types/api";

interface DashboardData {
  websites: WebsiteOverview[];
  incidents: Incident[];
  history: MonitoringResult[];
}

export function DashboardPage() {
  const [data, setData] = useState<DashboardData>({ websites: [], incidents: [], history: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadDashboard = useCallback(async () => {
    setError(null);
    try {
      const websites = await websiteApi.list();
      const [statsResults, incidentResults, historyResults] = await Promise.all([
        Promise.allSettled(websites.map((website) => websiteApi.stats(website.id))),
        Promise.allSettled(websites.map((website) => websiteApi.incidents(website.id))),
        Promise.allSettled(websites.map((website) => websiteApi.history(website.id))),
      ]);

      setData({
        websites: websites.map((website, index) => ({
          website,
          stats: statsResults[index].status === "fulfilled" ? statsResults[index].value : null,
        })),
        incidents: incidentResults
          .flatMap((result) => (result.status === "fulfilled" ? result.value : []))
          .sort((a, b) => Date.parse(b.startedAt) - Date.parse(a.startedAt)),
        history: historyResults
          .flatMap((result) => (result.status === "fulfilled" ? result.value : []))
          .sort((a, b) => Date.parse(b.checkedAt) - Date.parse(a.checkedAt)),
      });
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not load the dashboard.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadDashboard();
  }, [loadDashboard]);

  const summary = useMemo(() => {
    const up = data.websites.filter(({ website }) => website.status === "UP").length;
    const down = data.websites.filter(({ website }) => website.status === "DOWN").length;
    const measured = data.websites.filter(({ stats }) => stats && stats.totalChecks > 0);
    const averageUptime = measured.length
      ? measured.reduce((sum, item) => sum + (item.stats?.uptimePercentage ?? 0), 0) / measured.length
      : null;
    const averageResponse = measured.length
      ? measured.reduce((sum, item) => sum + (item.stats?.averageResponseTime ?? 0), 0) / measured.length
      : null;
    return { up, down, averageUptime, averageResponse };
  }, [data.websites]);

  if (loading) {
    return <LoadingState rows={7} />;
  }

  if (error && data.websites.length === 0) {
    return <ErrorState message={error} onRetry={loadDashboard} />;
  }

  const overallHealthy = summary.down === 0 && summary.up > 0;

  return (
    <div className="space-y-8 pb-24 lg:pb-4">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-[-0.035em]">System overview</h1>
          <p className="mt-1 text-sm text-[var(--text-muted)]">
            Current availability and performance across every monitored endpoint.
          </p>
        </div>
        <AddWebsiteDialog onCreated={loadDashboard} />
      </div>

      {error ? <p className="rounded-md bg-[var(--danger-soft)] px-4 py-3 text-sm text-[var(--danger)]">{error}</p> : null}

      <section className="grid grid-cols-2 gap-x-5 gap-y-7 md:grid-cols-3 xl:grid-cols-5" aria-label="Monitoring metrics">
        <MetricCard label="Monitored" value={String(data.websites.length)} detail="Configured websites" icon={GlobeHemisphereWest} />
        <MetricCard label="Operational" value={String(summary.up)} detail="Currently responding" icon={CheckCircle} tone="success" />
        <MetricCard label="Unavailable" value={String(summary.down)} detail="Require attention" icon={WarningCircle} tone={summary.down ? "danger" : "default"} />
        <MetricCard label="Average uptime" value={formatPercentage(summary.averageUptime)} detail="Across measured websites" icon={Pulse} />
        <MetricCard label="Average response" value={formatResponseTime(summary.averageResponse)} detail="Mean response latency" icon={ClockCountdown} />
      </section>

      <section className="grid gap-5 xl:grid-cols-[minmax(0,1.6fr)_minmax(300px,0.7fr)]">
        <div className="surface p-4 sm:p-5">
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-semibold">Response time</h2>
              <p className="mt-1 text-xs text-[var(--text-muted)]">Latest checks across all websites</p>
            </div>
            <span className="font-mono-numbers text-xs text-[var(--text-muted)]">milliseconds</span>
          </div>
          <ResponseTimeChart data={data.history} compact />
        </div>

        <div className="surface flex flex-col justify-between p-5">
          <div>
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-semibold">Overall health</h2>
              <StatusBadge status={overallHealthy ? "UP" : summary.down > 0 ? "DOWN" : null} />
            </div>
            <p className="mt-8 text-3xl font-semibold tracking-[-0.045em]">
              {overallHealthy
                ? "All systems operational"
                : summary.down > 0
                  ? `${summary.down} ${summary.down === 1 ? "website needs" : "websites need"} attention`
                  : "Awaiting first checks"}
            </p>
            <p className="mt-3 text-sm leading-6 text-[var(--text-muted)]">
              {overallHealthy
                ? "No monitored website is currently reporting a failure."
                : "Open a website to review recent checks and incident context."}
            </p>
          </div>
          <Link to="/websites" className="mt-8 inline-flex items-center gap-2 text-sm font-semibold text-[var(--accent)] hover:underline">
            Review websites
            <ArrowRight size={15} weight="bold" />
          </Link>
        </div>
      </section>

      <section>
        <div className="mb-3 flex items-center justify-between">
          <div>
            <h2 className="text-sm font-semibold">Monitored websites</h2>
            <p className="mt-1 text-xs text-[var(--text-muted)]">Current state and latest check</p>
          </div>
          <Link to="/websites" className="text-xs font-semibold text-[var(--accent)] hover:underline">View all</Link>
        </div>
        <div className="surface overflow-hidden">
          {data.websites.length === 0 ? (
            <div className="px-5 py-10 text-center text-sm text-[var(--text-muted)]">Add a website to start monitoring.</div>
          ) : (
            data.websites.slice(0, 6).map(({ website, stats }) => (
              <Link
                key={website.id}
                to={`/websites/${website.id}`}
                className="grid gap-3 border-b border-[var(--border)] px-4 py-3.5 last:border-b-0 hover:bg-[var(--surface-muted)]/45 sm:grid-cols-[minmax(0,1fr)_auto_auto] sm:items-center sm:px-5"
              >
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold">{website.name}</p>
                  <p className="truncate text-xs text-[var(--text-muted)]">{hostname(website.url)}</p>
                </div>
                <div className="flex items-center gap-3">
                  <StatusBadge status={website.status} />
                  <span className="font-mono-numbers w-20 text-right text-xs text-[var(--text-muted)]">
                    {formatResponseTime(stats?.averageResponseTime)}
                  </span>
                </div>
                <span className="w-24 text-right text-xs text-[var(--text-muted)]">{formatRelativeTime(stats?.lastCheckedAt)}</span>
              </Link>
            ))
          )}
        </div>
      </section>

      <section>
        <div className="mb-3 flex items-center justify-between">
          <div>
            <h2 className="text-sm font-semibold">Recent incidents</h2>
            <p className="mt-1 text-xs text-[var(--text-muted)]">Latest availability transitions</p>
          </div>
          <Link to="/incidents" className="text-xs font-semibold text-[var(--accent)] hover:underline">View all</Link>
        </div>
        <IncidentList incidents={data.incidents} limit={5} />
      </section>
    </div>
  );
}
