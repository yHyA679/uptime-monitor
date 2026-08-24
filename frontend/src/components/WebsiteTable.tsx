import {
  ArrowSquareOut,
  GearSix,
  Pulse,
  Trash,
} from "@phosphor-icons/react";
import { Link } from "react-router-dom";
import { formatPercentage, formatRelativeTime, formatResponseTime, hostname } from "../lib/format";
import type { WebsiteOverview } from "../types/api";
import { EmptyState } from "./AsyncStates";
import { StatusBadge } from "./StatusBadge";

interface WebsiteTableProps {
  websites: WebsiteOverview[];
  onCheck: (id: number) => void;
  onConfigure: (id: number) => void;
  onDelete: (id: number) => void;
  pendingActionId?: number | null;
}

export function WebsiteTable({
  websites,
  onCheck,
  onConfigure,
  onDelete,
  pendingActionId,
}: WebsiteTableProps) {
  if (websites.length === 0) {
    return (
      <EmptyState
        title="No websites monitored"
        message="Add a website to begin collecting availability and response-time history."
      />
    );
  }

  return (
    <div className="surface overflow-hidden">
      <div className="overflow-x-auto scrollbar-thin">
        <table className="w-full min-w-[980px] border-collapse text-left">
          <thead>
            <tr className="border-b border-[var(--border)] bg-[var(--surface-muted)]/50 text-[11px] font-semibold text-[var(--text-muted)]">
              <th className="px-4 py-3">Website</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3 text-right">Uptime</th>
              <th className="px-4 py-3 text-right">Response</th>
              <th className="px-4 py-3">Last checked</th>
              <th className="px-4 py-3">Monitoring</th>
              <th className="px-4 py-3 text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {websites.map(({ website, stats }) => (
              <tr
                key={website.id}
                className="group border-b border-[var(--border)] last:border-b-0 hover:bg-[var(--surface-muted)]/45"
              >
                <td className="px-4 py-3.5">
                  <Link to={`/websites/${website.id}`} className="flex min-w-0 items-center gap-3">
                    <span className="grid size-8 shrink-0 place-items-center rounded-lg border border-[var(--border)] bg-[var(--surface)] text-xs font-semibold text-[var(--text-muted)]">
                      {website.name.slice(0, 1).toUpperCase()}
                    </span>
                    <span className="min-w-0">
                      <span className="block truncate text-sm font-semibold group-hover:underline">
                        {website.name}
                      </span>
                      <span className="block truncate text-xs text-[var(--text-muted)]">
                        {hostname(website.url)}
                      </span>
                    </span>
                  </Link>
                </td>
                <td className="px-4 py-3.5">
                  <StatusBadge status={website.status} />
                </td>
                <td className="font-mono-numbers px-4 py-3.5 text-right text-sm">
                  {formatPercentage(stats?.uptimePercentage)}
                </td>
                <td className="font-mono-numbers px-4 py-3.5 text-right text-sm">
                  {formatResponseTime(stats?.averageResponseTime)}
                </td>
                <td className="px-4 py-3.5 text-xs text-[var(--text-muted)]">
                  {formatRelativeTime(stats?.lastCheckedAt)}
                </td>
                <td className="px-4 py-3.5">
                  <StatusBadge status={website.enabled ? "ENABLED" : "PAUSED"} />
                </td>
                <td className="px-4 py-3.5">
                  <div className="flex justify-end gap-1">
                    <ActionButton
                      label="Run check"
                      onClick={() => onCheck(website.id)}
                      disabled={pendingActionId === website.id}
                    >
                      <Pulse size={16} weight="bold" />
                    </ActionButton>
                    <Link
                      to={`/websites/${website.id}`}
                      aria-label={`Open ${website.name}`}
                      title="Open details"
                      className="interactive grid size-8 place-items-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)] hover:text-[var(--text)]"
                    >
                      <ArrowSquareOut size={16} weight="bold" />
                    </Link>
                    <ActionButton label="Configure" onClick={() => onConfigure(website.id)}>
                      <GearSix size={16} weight="bold" />
                    </ActionButton>
                    <ActionButton label="Delete" onClick={() => onDelete(website.id)} danger>
                      <Trash size={16} weight="bold" />
                    </ActionButton>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function ActionButton({
  label,
  onClick,
  danger,
  disabled,
  children,
}: {
  label: string;
  onClick: () => void;
  danger?: boolean;
  disabled?: boolean;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      title={label}
      className={`interactive grid size-8 place-items-center rounded-md disabled:cursor-not-allowed disabled:opacity-40 ${
        danger
          ? "text-[var(--text-muted)] hover:bg-[var(--danger-soft)] hover:text-[var(--danger)]"
          : "text-[var(--text-muted)] hover:bg-[var(--surface-muted)] hover:text-[var(--text)]"
      }`}
    >
      {children}
    </button>
  );
}
