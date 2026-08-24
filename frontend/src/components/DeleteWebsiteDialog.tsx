import { useState } from "react";
import * as Dialog from "@radix-ui/react-dialog";
import { Warning } from "@phosphor-icons/react";
import { websiteApi } from "../api/client";
import type { Website } from "../types/api";

export function DeleteWebsiteDialog({
  website,
  open,
  onOpenChange,
  onDeleted,
}: {
  website: Website | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onDeleted: () => void | Promise<void>;
}) {
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleDelete() {
    if (!website) return;
    setSubmitting(true);
    setError(null);
    try {
      await websiteApi.remove(website.id);
      await onDeleted();
      onOpenChange(false);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not delete the website.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-40 bg-black/35 backdrop-blur-[2px]" />
        <Dialog.Content className="fixed left-1/2 top-1/2 z-50 w-[calc(100%-2rem)] max-w-sm -translate-x-1/2 -translate-y-1/2 rounded-[10px] border border-[var(--border)] bg-[var(--surface)] p-6 text-[var(--text)] shadow-[0_24px_80px_rgba(20,24,22,0.16)]">
          <span className="grid size-10 place-items-center rounded-lg bg-[var(--danger-soft)] text-[var(--danger)]">
            <Warning size={20} weight="fill" />
          </span>
          <Dialog.Title className="mt-4 text-lg font-semibold tracking-[-0.025em]">Delete {website?.name}?</Dialog.Title>
          <Dialog.Description className="mt-2 text-sm leading-6 text-[var(--text-muted)]">
            This removes the website, its monitoring history, and all recorded incidents. This action cannot be undone.
          </Dialog.Description>

          {error ? <p role="alert" className="mt-4 rounded-md bg-[var(--danger-soft)] px-3 py-2 text-sm text-[var(--danger)]">{error}</p> : null}

          <div className="mt-6 flex justify-end gap-2">
            <Dialog.Close asChild>
              <button type="button" className="interactive h-9 rounded-md border border-[var(--border-strong)] px-3.5 text-sm font-medium hover:bg-[var(--surface-muted)]">Cancel</button>
            </Dialog.Close>
            <button
              type="button"
              onClick={handleDelete}
              disabled={submitting}
              className="interactive h-9 rounded-md bg-[var(--danger)] px-3.5 text-sm font-semibold whitespace-nowrap text-white disabled:opacity-50"
            >
              {submitting ? "Deleting..." : "Delete website"}
            </button>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
