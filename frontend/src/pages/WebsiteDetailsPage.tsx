import {
  ArrowLeft,
  ArrowSquareOut,
  ClockCountdown,
  GearSix,
  Pulse,
  Timer,
} from "@phosphor-icons/react";
import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { websiteApi } from "../api/client";
import { ErrorState, LoadingState } from "../components/AsyncStates";
import { IncidentList } from "../components/IncidentList";
import { MetricCard } from "../components/MetricCard";
import { MonitoringHistory } from "../components/MonitoringHistory";
import { ResponseTimeChart } from "../components/ResponseTimeChart";
import { SettingsDialog } from "../components/SettingsDialog";
import { StatusBadge } from "../components/StatusBadge";
import {
  formatPercentage,
  formatRelativeTime,
  formatResponseTime,
} from "../lib/format";
import type { Incident, MonitoringResult, Website, WebsiteStats } from "../types/api";

interface DetailsData {
  website: Website;
  stats: WebsiteStats;
  history: MonitoringResult[];
  incidents: Incident[];
}

export function WebsiteDetailsPage() {
  const { websiteId } = useParams();
  const id = Number(websiteId);
  const [data, setData] = useState<DetailsData | null>(null);
  const [loading, setLoading] = useState(true);
  const [checking, setChecking] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadDetails = useCallback(async () => {
    if (!Number.isInteger(id)) {
      setError("The website identifier is invalid.");
      setLoading(false);
      return;
    }

    setError(null);
    try {
      const [website, stats, history, incidents] = await Promise.all([
        websiteApi.get(id),
        websiteApi.stats(id),
        websiteApi.history(id),
        websiteApi.incidents(id),
      ]);
      setData({ website, stats, history, incidents });
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not load website details.");
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    void loadDetails();
  }, [loadDetails]);

  async function runCheck() {
    setChecking(true);
    setError(null);
    try {
      await websiteApi.check(id);
      await loadDetails();
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "The manual check failed.");
    } finally {
      setChecking(false);
    }
  }

  if (loading) return <LoadingState rows={8} />;
  if (!data) return <ErrorState message={error ?? "Website not found."} onRetry={loadDetails} />;

  const { website, stats, history, incidents } = data;

  return (
    <div className="space-y-8 pb-24 lg:pb-4">
      <div>
        <Link to="/websites" className="inline-flex items-center gap-2 text-xs font-semibold text-[var(--text-muted)] hover:text-[var(--text)]">
          <ArrowLeft size={14} weight="bold" />
          All websites
        </Link>
        <div className="mt-4 flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-3">
              <h1 className="text-3xl font-semibold tracking-[-0.045em]">{website.name}</h1>
              <StatusBadge status={website.status} />
              {!website.enabled ? <StatusBadge status="PAUSED" /> : null}
            </div>
            <a
              href={website.url}
              target="_blank"
              rel="noreferrer"
              className="mt-2 inline-flex max-w-full items-center gap-2 truncate text-sm text-[var(--text-muted)] hover:text-[var(--accent)]"
            >
              <span className="truncate">{website.url}</span>
              <ArrowSquareOut size={14} weight="bold" className="shrink-0" />
            </a>
          </div>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => setSettingsOpen(true)}
              className="interactive inline-flex h-9 items-center gap-2 rounded-md border border-[var(--border-strong)] bg-[var(--surface)] px-3.5 text-sm font-medium hover:bg-[var(--surface-muted)]"
            >
              <GearSix size={16} weight="bold" />
              Configure
            </button>
            <button
              type="button"
              onClick={runCheck}
              disabled={checking}
              className="interactive inline-flex h-9 items-center gap-2 rounded-md bg-[var(--text)] px-3.5 text-sm font-semibold whitespace-nowrap text-[var(--surface)] disabled:opacity-50"
            >
              <Pulse size={16} weight="bold" />
              {checking ? "Checking..." : "Run check"}
            </button>
          </div>
        </div>
      </div>

      {error ? <p role="alert" className="rounded-md bg-[var(--danger-soft)] px-4 py-3 text-sm text-[var(--danger)]">{error}</p> : null}

      <section className="grid grid-cols-2 gap-x-5 gap-y-7 lg:grid-cols-4" aria-label="Website metrics">
        <MetricCard label="Uptime" value={formatPercentage(stats.uptimePercentage)} detail={`${stats.upChecks} successful checks`} icon={Pulse} />
        <MetricCard label="Average response" value={formatResponseTime(stats.averageResponseTime)} detail="Across all recorded checks" icon={ClockCountdown} />
        <MetricCard label="Total checks" value={String(stats.totalChecks)} detail={`${stats.downChecks} failed checks`} icon={Timer} tone={stats.downChecks ? "danger" : "default"} />
        <MetricCard label="Last checked" value={formatRelativeTime(stats.lastCheckedAt)} detail={website.enabled ? `Every ${website.checkIntervalSeconds} seconds` : "Automatic checks paused"} icon={GearSix} />
      </section>

      <section className="surface p-4 sm:p-5">
        <div className="mb-5 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-sm font-semibold">Response-time history</h2>
            <p className="mt-1 text-xs text-[var(--text-muted)]">Most recent 48 checks, oldest to newest</p>
          </div>
          <span className="font-mono-numbers text-xs text-[var(--text-muted)]">milliseconds</span>
        </div>
        <ResponseTimeChart data={history} />
      </section>

      <section className="grid min-w-0 gap-5 xl:grid-cols-[minmax(0,1.45fr)_minmax(300px,0.55fr)]">
        <div className="min-w-0">
          <div className="mb-3">
            <h2 className="text-sm font-semibold">Monitoring history</h2>
            <p className="mt-1 text-xs text-[var(--text-muted)]">Newest checks appear first</p>
          </div>
          <MonitoringHistory history={history} />
        </div>

        <aside>
          <div className="mb-3">
            <h2 className="text-sm font-semibold">Monitoring configuration</h2>
            <p className="mt-1 text-xs text-[var(--text-muted)]">Scheduler settings for this website</p>
          </div>
          <div className="surface p-5">
            <dl className="space-y-5">
              <div className="flex items-center justify-between gap-4">
                <dt className="text-sm text-[var(--text-muted)]">Automatic checks</dt>
                <dd><StatusBadge status={website.enabled ? "ENABLED" : "PAUSED"} /></dd>
              </div>
              <div className="flex items-center justify-between gap-4">
                <dt className="text-sm text-[var(--text-muted)]">Interval</dt>
                <dd className="font-mono-numbers text-sm font-semibold">{website.checkIntervalSeconds}s</dd>
              </div>
              <div className="flex items-center justify-between gap-4">
                <dt className="text-sm text-[var(--text-muted)]">Open incidents</dt>
                <dd className="font-mono-numbers text-sm font-semibold">{incidents.filter((incident) => incident.status === "OPEN").length}</dd>
              </div>
            </dl>
            <button
              type="button"
              onClick={() => setSettingsOpen(true)}
              className="interactive mt-6 h-9 w-full rounded-md border border-[var(--border-strong)] text-sm font-semibold hover:bg-[var(--surface-muted)]"
            >
              Edit settings
            </button>
          </div>
        </aside>
      </section>

      <section>
        <div className="mb-3">
          <h2 className="text-sm font-semibold">Incidents</h2>
          <p className="mt-1 text-xs text-[var(--text-muted)]">Availability loss and recovery periods</p>
        </div>
        <IncidentList incidents={incidents} showWebsite={false} />
      </section>

      <SettingsDialog
        website={website}
        open={settingsOpen}
        onOpenChange={setSettingsOpen}
        onSaved={loadDetails}
      />
    </div>
  );
}
