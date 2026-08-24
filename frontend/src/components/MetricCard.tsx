import type { Icon } from "@phosphor-icons/react";

interface MetricCardProps {
  label: string;
  value: string;
  detail: string;
  icon: Icon;
  tone?: "default" | "success" | "danger";
}

export function MetricCard({ label, value, detail, icon: Icon, tone = "default" }: MetricCardProps) {
  const toneClass =
    tone === "success"
      ? "text-[var(--success)]"
      : tone === "danger"
        ? "text-[var(--danger)]"
        : "text-[var(--accent)]";

  return (
    <article className="border-t border-[var(--border-strong)] pt-4">
      <div className="mb-6 flex items-center justify-between">
        <p className="text-xs font-medium text-[var(--text-muted)]">{label}</p>
        <Icon size={17} weight="bold" className={toneClass} aria-hidden="true" />
      </div>
      <p className="font-mono-numbers text-3xl font-semibold tracking-[-0.05em]">{value}</p>
      <p className="mt-2 text-xs text-[var(--text-muted)]">{detail}</p>
    </article>
  );
}
