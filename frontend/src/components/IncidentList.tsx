import { ArrowRight, Clock } from "@phosphor-icons/react";
import { Link } from "react-router-dom";
import { formatDateTime, formatDuration } from "../lib/format";
import type { Incident } from "../types/api";
import { EmptyState } from "./AsyncStates";
import { StatusBadge } from "./StatusBadge";

export function IncidentList({
  incidents,
  limit,
  showWebsite = true,
}: {
  incidents: Incident[];
  limit?: number;
  showWebsite?: boolean;
}) {
  const visible = limit ? incidents.slice(0, limit) : incidents;

  if (visible.length === 0) {
    return (
      <EmptyState
        title="No incidents recorded"
        message="Incidents appear after a monitored website changes from UP to DOWN."
      />
    );
  }

  return (
    <div className="surface overflow-hidden">
      {visible.map((incident) => (
        <article
          key={incident.id}
          className="grid gap-3 border-b border-[var(--border)] px-4 py-4 last:border-b-0 sm:grid-cols-[1fr_auto] sm:items-center sm:px-5"
        >
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <StatusBadge status={incident.status} />
              {showWebsite ? (
                <Link
                  to={`/websites/${incident.website.id}`}
                  className="truncate text-sm font-semibold hover:underline"
                >
                  {incident.website.name}
                </Link>
              ) : (
                <p className="text-sm font-semibold">
                  {incident.status === "OPEN" ? "Availability lost" : "Availability restored"}
                </p>
              )}
            </div>
            <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-[var(--text-muted)]">
              <span>{formatDateTime(incident.startedAt)}</span>
              <ArrowRight size={12} weight="bold" aria-hidden="true" />
              <span>{incident.resolvedAt ? formatDateTime(incident.resolvedAt) : "Still open"}</span>
            </div>
          </div>
          <div className="flex items-center gap-2 text-xs text-[var(--text-muted)] sm:justify-end">
            <Clock size={14} weight="bold" aria-hidden="true" />
            <span className="font-mono-numbers">{formatDuration(incident.durationSeconds)}</span>
          </div>
        </article>
      ))}
    </div>
  );
}
