import { useEffect, useState, type FormEvent } from "react";
import * as Dialog from "@radix-ui/react-dialog";
import * as Switch from "@radix-ui/react-switch";
import { X } from "@phosphor-icons/react";
import { websiteApi } from "../api/client";
import type { Website } from "../types/api";

export function SettingsDialog({
  website,
  open,
  onOpenChange,
  onSaved,
}: {
  website: Website | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSaved: () => void | Promise<void>;
}) {
  const [interval, setInterval] = useState(60);
  const [enabled, setEnabled] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (website) {
      setInterval(website.checkIntervalSeconds);
      setEnabled(website.enabled);
      setError(null);
    }
  }, [website]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!website) return;

    if (interval < 10 || interval > 3600) {
      setError("Monitoring interval must be between 10 and 3600 seconds.");
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      await websiteApi.updateSettings(website.id, {
        checkIntervalSeconds: interval,
        enabled,
      });
      await onSaved();
      onOpenChange(false);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not save settings.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-40 bg-black/35 backdrop-blur-[2px]" />
        <Dialog.Content className="fixed left-1/2 top-1/2 z-50 w-[calc(100%-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 rounded-[10px] border border-[var(--border)] bg-[var(--surface)] p-5 text-[var(--text)] shadow-[0_24px_80px_rgba(20,24,22,0.16)] sm:p-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <Dialog.Title className="text-lg font-semibold tracking-[-0.025em]">Monitoring settings</Dialog.Title>
              <Dialog.Description className="mt-1 text-sm text-[var(--text-muted)]">
                {website?.name ?? "Selected website"}
              </Dialog.Description>
            </div>
            <Dialog.Close asChild>
              <button type="button" className="grid size-8 place-items-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)]" aria-label="Close">
                <X size={17} weight="bold" />
              </button>
            </Dialog.Close>
          </div>

          <form onSubmit={handleSubmit} className="mt-6 space-y-5">
            <div className="space-y-2">
              <div className="flex items-baseline justify-between">
                <label htmlFor="settings-interval" className="text-sm font-medium">Monitoring interval</label>
                <span className="text-xs text-[var(--text-muted)]">10 to 3600 seconds</span>
              </div>
              <div className="relative">
                <input
                  id="settings-interval"
                  type="number"
                  min={10}
                  max={3600}
                  value={interval}
                  onChange={(event) => setInterval(Number(event.target.value))}
                  className="font-mono-numbers h-10 w-full rounded-md border border-[var(--border-strong)] bg-[var(--canvas)] px-3 pr-20 text-sm"
                />
                <span className="pointer-events-none absolute inset-y-0 right-3 flex items-center text-xs text-[var(--text-muted)]">seconds</span>
              </div>
            </div>

            <div className="flex items-center justify-between rounded-lg border border-[var(--border)] bg-[var(--canvas)] px-3 py-3">
              <div>
                <label htmlFor="settings-enabled" className="text-sm font-medium">Automatic monitoring</label>
                <p className="mt-0.5 text-xs text-[var(--text-muted)]">Manual checks remain available when paused.</p>
              </div>
              <Switch.Root
                id="settings-enabled"
                checked={enabled}
                onCheckedChange={setEnabled}
                className="relative h-6 w-10 rounded-full bg-[var(--border-strong)] data-[state=checked]:bg-[var(--accent)]"
              >
                <Switch.Thumb className="block size-5 translate-x-0.5 rounded-full bg-white transition-transform data-[state=checked]:translate-x-[18px]" />
              </Switch.Root>
            </div>

            <p className="text-xs leading-5 text-[var(--text-muted)]">
              Changing the website name or URL requires a backend update endpoint that is not available yet.
            </p>

            {error ? <p role="alert" className="rounded-md bg-[var(--danger-soft)] px-3 py-2 text-sm text-[var(--danger)]">{error}</p> : null}

            <div className="flex justify-end gap-2 pt-1">
              <Dialog.Close asChild>
                <button type="button" className="interactive h-9 rounded-md border border-[var(--border-strong)] px-3.5 text-sm font-medium hover:bg-[var(--surface-muted)]">Cancel</button>
              </Dialog.Close>
              <button type="submit" disabled={submitting} className="interactive h-9 rounded-md bg-[var(--text)] px-3.5 text-sm font-semibold whitespace-nowrap text-[var(--surface)] disabled:opacity-50">
                {submitting ? "Saving..." : "Save settings"}
              </button>
            </div>
          </form>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
