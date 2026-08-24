import { useState, type FormEvent } from "react";
import * as Dialog from "@radix-ui/react-dialog";
import * as Switch from "@radix-ui/react-switch";
import { Plus, X } from "@phosphor-icons/react";
import { websiteApi } from "../api/client";

export function AddWebsiteDialog({ onCreated }: { onCreated: () => void | Promise<void> }) {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [url, setUrl] = useState("");
  const [interval, setInterval] = useState(60);
  const [enabled, setEnabled] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);

    if (!name.trim()) {
      setError("Enter a website name.");
      return;
    }

    try {
      new URL(url);
    } catch {
      setError("Enter a complete URL including http:// or https://.");
      return;
    }

    if (interval < 10 || interval > 3600) {
      setError("Monitoring interval must be between 10 and 3600 seconds.");
      return;
    }

    setSubmitting(true);
    try {
      await websiteApi.create({
        name: name.trim(),
        url: url.trim(),
        checkIntervalSeconds: interval,
        enabled,
      });
      await onCreated();
      setOpen(false);
      setName("");
      setUrl("");
      setInterval(60);
      setEnabled(true);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not add the website.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog.Root open={open} onOpenChange={setOpen}>
      <Dialog.Trigger asChild>
        <button
          type="button"
          className="interactive inline-flex h-9 items-center gap-2 rounded-md bg-[var(--text)] px-3.5 text-sm font-semibold whitespace-nowrap text-[var(--surface)] hover:opacity-90"
        >
          <Plus size={16} weight="bold" />
          Add website
        </button>
      </Dialog.Trigger>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-40 bg-black/35 backdrop-blur-[2px]" />
        <Dialog.Content className="fixed left-1/2 top-1/2 z-50 w-[calc(100%-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 rounded-[10px] border border-[var(--border)] bg-[var(--surface)] p-5 text-[var(--text)] shadow-[0_24px_80px_rgba(20,24,22,0.16)] sm:p-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <Dialog.Title className="text-lg font-semibold tracking-[-0.025em]">Add website</Dialog.Title>
              <Dialog.Description className="mt-1 text-sm leading-6 text-[var(--text-muted)]">
                Start collecting availability and response-time checks.
              </Dialog.Description>
            </div>
            <Dialog.Close asChild>
              <button
                type="button"
                className="interactive grid size-8 shrink-0 place-items-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)]"
                aria-label="Close"
              >
                <X size={17} weight="bold" />
              </button>
            </Dialog.Close>
          </div>

          <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
            <Field label="Name" htmlFor="website-name">
              <input
                id="website-name"
                value={name}
                onChange={(event) => setName(event.target.value)}
                className="h-10 w-full rounded-md border border-[var(--border-strong)] bg-[var(--canvas)] px-3 text-sm placeholder:text-[var(--text-muted)]"
                placeholder="Status page"
                autoFocus
              />
            </Field>

            <Field label="URL" htmlFor="website-url" hint="Include the protocol.">
              <input
                id="website-url"
                type="url"
                value={url}
                onChange={(event) => setUrl(event.target.value)}
                className="h-10 w-full rounded-md border border-[var(--border-strong)] bg-[var(--canvas)] px-3 text-sm placeholder:text-[var(--text-muted)]"
                placeholder="https://example.com"
              />
            </Field>

            <Field label="Monitoring interval" htmlFor="website-interval" hint="10 to 3600 seconds.">
              <div className="relative">
                <input
                  id="website-interval"
                  type="number"
                  min={10}
                  max={3600}
                  value={interval}
                  onChange={(event) => setInterval(Number(event.target.value))}
                  className="font-mono-numbers h-10 w-full rounded-md border border-[var(--border-strong)] bg-[var(--canvas)] px-3 pr-20 text-sm"
                />
                <span className="pointer-events-none absolute inset-y-0 right-3 flex items-center text-xs text-[var(--text-muted)]">
                  seconds
                </span>
              </div>
            </Field>

            <div className="flex items-center justify-between rounded-lg border border-[var(--border)] bg-[var(--canvas)] px-3 py-3">
              <div>
                <label htmlFor="website-enabled" className="text-sm font-medium">
                  Automatic monitoring
                </label>
                <p className="mt-0.5 text-xs text-[var(--text-muted)]">Run scheduled checks after creation.</p>
              </div>
              <Switch.Root
                id="website-enabled"
                checked={enabled}
                onCheckedChange={setEnabled}
                className="relative h-6 w-10 rounded-full bg-[var(--border-strong)] data-[state=checked]:bg-[var(--accent)]"
              >
                <Switch.Thumb className="block size-5 translate-x-0.5 rounded-full bg-white transition-transform data-[state=checked]:translate-x-[18px]" />
              </Switch.Root>
            </div>

            {error ? (
              <p role="alert" className="rounded-md bg-[var(--danger-soft)] px-3 py-2 text-sm text-[var(--danger)]">
                {error}
              </p>
            ) : null}

            <div className="flex justify-end gap-2 pt-2">
              <Dialog.Close asChild>
                <button
                  type="button"
                  className="interactive h-9 rounded-md border border-[var(--border-strong)] px-3.5 text-sm font-medium hover:bg-[var(--surface-muted)]"
                >
                  Cancel
                </button>
              </Dialog.Close>
              <button
                type="submit"
                disabled={submitting}
                className="interactive h-9 rounded-md bg-[var(--text)] px-3.5 text-sm font-semibold whitespace-nowrap text-[var(--surface)] disabled:opacity-50"
              >
                {submitting ? "Adding..." : "Add website"}
              </button>
            </div>
          </form>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

function Field({
  label,
  htmlFor,
  hint,
  children,
}: {
  label: string;
  htmlFor: string;
  hint?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-2">
      <div className="flex items-baseline justify-between gap-3">
        <label htmlFor={htmlFor} className="text-sm font-medium">
          {label}
        </label>
        {hint ? <span className="text-xs text-[var(--text-muted)]">{hint}</span> : null}
      </div>
      {children}
    </div>
  );
}
