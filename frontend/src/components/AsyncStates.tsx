import { ArrowClockwise, Database, WarningCircle } from "@phosphor-icons/react";

export function LoadingState({ rows = 5 }: { rows?: number }) {
  return (
    <div className="surface overflow-hidden" aria-label="Loading" aria-busy="true">
      {Array.from({ length: rows }).map((_, index) => (
        <div key={index} className="flex items-center gap-4 border-b border-[var(--border)] px-5 py-4 last:border-b-0">
          <div className="size-8 animate-pulse rounded-lg bg-[var(--surface-muted)]" />
          <div className="flex-1 space-y-2">
            <div className="h-3 w-36 animate-pulse rounded bg-[var(--surface-muted)]" />
            <div className="h-2.5 w-52 max-w-full animate-pulse rounded bg-[var(--surface-muted)]" />
          </div>
          <div className="h-6 w-16 animate-pulse rounded bg-[var(--surface-muted)]" />
        </div>
      ))}
    </div>
  );
}

export function EmptyState({
  title,
  message,
  action,
}: {
  title: string;
  message: string;
  action?: React.ReactNode;
}) {
  return (
    <div className="surface flex min-h-56 flex-col items-center justify-center px-6 py-10 text-center">
      <span className="mb-4 grid size-10 place-items-center rounded-lg bg-[var(--surface-muted)] text-[var(--text-muted)]">
        <Database size={20} weight="bold" aria-hidden="true" />
      </span>
      <h3 className="text-sm font-semibold">{title}</h3>
      <p className="mt-2 max-w-sm text-sm leading-6 text-[var(--text-muted)]">{message}</p>
      {action ? <div className="mt-5">{action}</div> : null}
    </div>
  );
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="surface flex min-h-48 flex-col items-center justify-center px-6 py-10 text-center">
      <WarningCircle size={24} weight="fill" className="text-[var(--danger)]" aria-hidden="true" />
      <h3 className="mt-3 text-sm font-semibold">Could not load monitoring data</h3>
      <p className="mt-2 max-w-md text-sm text-[var(--text-muted)]">{message}</p>
      {onRetry ? (
        <button
          type="button"
          onClick={onRetry}
          className="interactive mt-5 inline-flex h-9 items-center gap-2 rounded-md border border-[var(--border-strong)] bg-[var(--surface)] px-3 text-sm font-medium hover:bg-[var(--surface-muted)]"
        >
          <ArrowClockwise size={15} weight="bold" />
          Retry
        </button>
      ) : null}
    </div>
  );
}
