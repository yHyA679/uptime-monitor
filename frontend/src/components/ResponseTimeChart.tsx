import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { MonitoringResult } from "../types/api";

export function ResponseTimeChart({
  data,
  compact = false,
}: {
  data: MonitoringResult[];
  compact?: boolean;
}) {
  const chartData = data
    .slice(0, compact ? 18 : 48)
    .reverse()
    .map((result) => ({
      checkedAt: result.checkedAt,
      responseTime: result.responseTime,
      status: result.status,
    }));

  if (chartData.length === 0) {
    return (
      <div className="grid h-full min-h-48 place-items-center text-sm text-[var(--text-muted)]">
        Response-time data will appear after the first check.
      </div>
    );
  }

  return (
    <div className={compact ? "h-48" : "h-72"} aria-label="Response time over time">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={chartData} margin={{ top: 8, right: 8, bottom: 0, left: compact ? -24 : -12 }}>
          <CartesianGrid vertical={false} stroke="var(--border)" strokeDasharray="2 4" />
          <XAxis
            dataKey="checkedAt"
            tickFormatter={(value: string) =>
              new Intl.DateTimeFormat(undefined, {
                hour: "2-digit",
                minute: "2-digit",
              }).format(new Date(value))
            }
            axisLine={false}
            tickLine={false}
            tick={{ fill: "var(--text-muted)", fontSize: 10 }}
            minTickGap={32}
            hide={compact}
          />
          <YAxis
            axisLine={false}
            tickLine={false}
            tick={{ fill: "var(--text-muted)", fontSize: 10 }}
            width={44}
            tickFormatter={(value: number) => `${value}`}
          />
          <Tooltip
            cursor={{ stroke: "var(--border-strong)", strokeWidth: 1 }}
            contentStyle={{
              background: "var(--surface)",
              border: "1px solid var(--border)",
              borderRadius: 8,
              boxShadow: "none",
              fontSize: 12,
            }}
            labelFormatter={(value) =>
              new Intl.DateTimeFormat(undefined, {
                month: "short",
                day: "numeric",
                hour: "2-digit",
                minute: "2-digit",
                second: "2-digit",
              }).format(new Date(String(value)))
            }
            formatter={(value) => [`${value} ms`, "Response time"]}
          />
          <Line
            type="monotone"
            dataKey="responseTime"
            stroke="var(--accent)"
            strokeWidth={2}
            dot={false}
            activeDot={{ r: 4, fill: "var(--accent)", stroke: "var(--surface)", strokeWidth: 2 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
