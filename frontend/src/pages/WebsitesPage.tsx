import { FunnelSimple, MagnifyingGlass } from "@phosphor-icons/react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { websiteApi } from "../api/client";
import { AddWebsiteDialog } from "../components/AddWebsiteDialog";
import { ErrorState, LoadingState } from "../components/AsyncStates";
import { DeleteWebsiteDialog } from "../components/DeleteWebsiteDialog";
import { SettingsDialog } from "../components/SettingsDialog";
import { WebsiteTable } from "../components/WebsiteTable";
import type { Website, WebsiteOverview } from "../types/api";

type Filter = "ALL" | "UP" | "DOWN" | "PAUSED";

export function WebsitesPage() {
  const [websites, setWebsites] = useState<WebsiteOverview[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState<Filter>("ALL");
  const [pendingActionId, setPendingActionId] = useState<number | null>(null);
  const [settingsWebsite, setSettingsWebsite] = useState<Website | null>(null);
  const [deleteWebsite, setDeleteWebsite] = useState<Website | null>(null);

  const loadWebsites = useCallback(async () => {
    setError(null);
    try {
      const items = await websiteApi.list();
      const stats = await Promise.allSettled(items.map((website) => websiteApi.stats(website.id)));
      setWebsites(
        items.map((website, index) => ({
          website,
          stats: stats[index].status === "fulfilled" ? stats[index].value : null,
        })),
      );
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not load websites.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadWebsites();
  }, [loadWebsites]);

  const filteredWebsites = useMemo(() => {
    const query = search.trim().toLowerCase();
    return websites.filter(({ website }) => {
      const matchesSearch =
        !query ||
        website.name.toLowerCase().includes(query) ||
        website.url.toLowerCase().includes(query);
      const matchesFilter =
        filter === "ALL" ||
        (filter === "PAUSED" ? !website.enabled : website.status === filter);
      return matchesSearch && matchesFilter;
    });
  }, [filter, search, websites]);

  async function handleCheck(id: number) {
    setPendingActionId(id);
    setError(null);
    try {
      await websiteApi.check(id);
      await loadWebsites();
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "The manual check failed.");
    } finally {
      setPendingActionId(null);
    }
  }

  return (
    <div className="pb-24 lg:pb-4">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-[-0.035em]">Monitored websites</h1>
          <p className="mt-1 text-sm text-[var(--text-muted)]">
            Configure checks, inspect current state, and run manual probes.
          </p>
        </div>
        <AddWebsiteDialog onCreated={loadWebsites} />
      </div>

      <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="relative w-full sm:max-w-sm">
          <MagnifyingGlass size={16} weight="bold" className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)]" />
          <input
            type="search"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search by name or URL"
            aria-label="Search websites"
            className="h-9 w-full rounded-md border border-[var(--border)] bg-[var(--surface)] pl-9 pr-3 text-sm placeholder:text-[var(--text-muted)]"
          />
        </div>
        <div className="flex items-center gap-1 overflow-x-auto" aria-label="Filter websites">
          <FunnelSimple size={15} weight="bold" className="mr-1 shrink-0 text-[var(--text-muted)]" />
          {(["ALL", "UP", "DOWN", "PAUSED"] as Filter[]).map((option) => (
            <button
              key={option}
              type="button"
              onClick={() => setFilter(option)}
              className={`interactive h-8 rounded-md px-3 text-xs font-semibold ${
                filter === option
                  ? "bg-[var(--text)] text-[var(--surface)]"
                  : "text-[var(--text-muted)] hover:bg-[var(--surface-muted)] hover:text-[var(--text)]"
              }`}
            >
              {option === "ALL" ? "All" : option.charAt(0) + option.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
      </div>

      {error ? (
        <div role="alert" className="mt-4 rounded-md border border-[var(--danger)]/30 bg-[var(--danger-soft)] px-4 py-3 text-sm text-[var(--danger)]">
          {error}
        </div>
      ) : null}

      <div className="mt-4">
        {loading ? (
          <LoadingState rows={6} />
        ) : error && websites.length === 0 ? (
          <ErrorState message={error} onRetry={loadWebsites} />
        ) : (
          <WebsiteTable
            websites={filteredWebsites}
            onCheck={handleCheck}
            onConfigure={(id) =>
              setSettingsWebsite(websites.find(({ website }) => website.id === id)?.website ?? null)
            }
            onDelete={(id) =>
              setDeleteWebsite(websites.find(({ website }) => website.id === id)?.website ?? null)
            }
            pendingActionId={pendingActionId}
          />
        )}
      </div>

      <SettingsDialog
        website={settingsWebsite}
        open={settingsWebsite !== null}
        onOpenChange={(open) => !open && setSettingsWebsite(null)}
        onSaved={loadWebsites}
      />
      <DeleteWebsiteDialog
        website={deleteWebsite}
        open={deleteWebsite !== null}
        onOpenChange={(open) => !open && setDeleteWebsite(null)}
        onDeleted={loadWebsites}
      />
    </div>
  );
}
