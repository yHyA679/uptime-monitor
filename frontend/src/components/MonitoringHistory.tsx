import { formatDateTime } from "../lib/format";
import type { MonitoringResult } from "../types/api";
import { EmptyState } from "./AsyncStates";
import { StatusBadge } from "./StatusBadge";

export function MonitoringHistory({ history }: { history: MonitoringResult[] }) {
  if (history.length === 0) {
    return (
      <EmptyState
        title="No checks yet"
        message="Run a manual check or wait for automatic monitoring to record the first result."
      />
    );
  }

  return (
    <div className="surface overflow-hidden">
      <div className="overflow-x-auto scrollbar-thin">
        <table className="w-full min-w-[520px] border-collapse text-left">
          <thead>
            <tr className="border-b border-[var(--border)] bg-[var(--surface-muted)]/50 text-[11px] font-semibold text-[var(--text-muted)]">
              <th className="px-4 py-3">Checked at</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3 text-right">Response time</th>
            </tr>
          </thead>
          <tbody>
            {history.map((result) => (
              <tr key={result.id} className="border-b border-[var(--border)] last:border-b-0">
                <td className="px-4 py-3 text-sm">{formatDateTime(result.checkedAt)}</td>
                <td className="px-4 py-3">
                  <StatusBadge status={result.status} />
                </td>
                <td className="font-mono-numbers px-4 py-3 text-right text-sm">
                  {result.responseTime} ms
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
