import { CheckCircle, Siren, WarningCircle } from "@phosphor-icons/react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { websiteApi } from "../api/client";
import { ErrorState, LoadingState } from "../components/AsyncStates";
import { IncidentList } from "../components/IncidentList";
import { MetricCard } from "../components/MetricCard";
import type { Incident, IncidentStatus } from "../types/api";

type Filter = "ALL" | IncidentStatus;

export function IncidentsPage() {
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [filter, setFilter] = useState<Filter>("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadIncidents = useCallback(async () => {
    setError(null);
    try {
      const websites = await websiteApi.list();
      const results = await Promise.allSettled(websites.map((website) => websiteApi.incidents(website.id)));
      setIncidents(
        results
          .flatMap((result) => (result.status === "fulfilled" ? result.value : []))
          .sort((a, b) => Date.parse(b.startedAt) - Date.parse(a.startedAt)),
      );
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not load incidents.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadIncidents();
  }, [loadIncidents]);

  const openCount = incidents.filter((incident) => incident.status === "OPEN").length;
  const resolvedCount = incidents.length - openCount;
  const filteredIncidents = useMemo(
    () => incidents.filter((incident) => filter === "ALL" || incident.status === filter),
    [filter, incidents],
  );

  if (loading) return <LoadingState rows={7} />;
  if (error && incidents.length === 0) return <ErrorState message={error} onRetry={loadIncidents} />;

  return (
    <div className="space-y-8 pb-24 lg:pb-4">
      <div>
        <h1 className="text-2xl font-semibold tracking-[-0.035em]">Incidents</h1>
        <p className="mt-1 text-sm text-[var(--text-muted)]">
          Availability transitions recorded from website checks.
        </p>
      </div>

      <section className="grid grid-cols-2 gap-x-5 gap-y-7 md:grid-cols-3" aria-label="Incident metrics">
        <MetricCard label="All incidents" value={String(incidents.length)} detail="Recorded availability events" icon={Siren} />
        <MetricCard label="Open" value={String(openCount)} detail="Currently unresolved" icon={WarningCircle} tone={openCount ? "danger" : "default"} />
        <MetricCard label="Resolved" value={String(resolvedCount)} detail="Recovered websites" icon={CheckCircle} tone="success" />
      </section>

      {error ? <p className="rounded-md bg-[var(--danger-soft)] px-4 py-3 text-sm text-[var(--danger)]">{error}</p> : null}

      <section>
        <div className="mb-3 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-sm font-semibold">Incident timeline</h2>
            <p className="mt-1 text-xs text-[var(--text-muted)]">Newest incidents appear first</p>
          </div>
          <div className="flex items-center gap-1" aria-label="Filter incidents">
            {(["ALL", "OPEN", "RESOLVED"] as Filter[]).map((option) => (
              <button
                key={option}
                type="button"
                onClick={() => setFilter(option)}
                className={`interactive h-8 rounded-md px-3 text-xs font-semibold ${
                  filter === option
                    ? "bg-[var(--text)] text-[var(--surface)]"
                    : "text-[var(--text-muted)] hover:bg-[var(--surface-muted)] hover:text-[var(--text)]"
                }`}
              >
                {option === "ALL" ? "All" : option.charAt(0) + option.slice(1).toLowerCase()}
              </button>
            ))}
          </div>
        </div>
        <IncidentList incidents={filteredIncidents} />
      </section>
    </div>
  );
}
