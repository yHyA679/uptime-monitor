import type { IncidentStatus, WebsiteStatus } from "../types/api";

type Status = WebsiteStatus | IncidentStatus | "ENABLED" | "PAUSED";

const styles: Record<Exclude<Status, null>, string> = {
  UP: "bg-[var(--success-soft)] text-[var(--success)]",
  DOWN: "bg-[var(--danger-soft)] text-[var(--danger)]",
  OPEN: "bg-[var(--danger-soft)] text-[var(--danger)]",
  RESOLVED: "bg-[var(--success-soft)] text-[var(--success)]",
  ENABLED: "bg-[var(--success-soft)] text-[var(--success)]",
  PAUSED: "bg-[var(--warning-soft)] text-[var(--warning)]",
};

export function StatusBadge({ status }: { status: Status }) {
  const label = status ?? "PENDING";
  const className = status
    ? styles[status]
    : "bg-[var(--surface-muted)] text-[var(--text-muted)]";

  return (
    <span className={`inline-flex items-center rounded-full px-2 py-1 text-[10px] font-bold tracking-[0.06em] ${className}`}>
      {label}
    </span>
  );
}
