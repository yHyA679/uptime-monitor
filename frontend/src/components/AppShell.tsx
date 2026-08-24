import {
  ChartLineUp,
  GlobeHemisphereWest,
  Moon,
  Pulse,
  Siren,
  Sun,
} from "@phosphor-icons/react";
import { NavLink, Outlet, useLocation } from "react-router-dom";
import * as Tooltip from "@radix-ui/react-tooltip";
import { useTheme } from "../hooks/useTheme";

const navigation = [
  { label: "Dashboard", to: "/", icon: ChartLineUp, end: true },
  { label: "Websites", to: "/websites", icon: GlobeHemisphereWest },
  { label: "Incidents", to: "/incidents", icon: Siren },
];

function pageTitle(pathname: string): string {
  if (pathname.startsWith("/websites/") && pathname !== "/websites") return "Website details";
  if (pathname.startsWith("/websites")) return "Websites";
  if (pathname.startsWith("/incidents")) return "Incidents";
  return "Dashboard";
}

export function AppShell() {
  const location = useLocation();
  const { theme, toggleTheme } = useTheme();

  return (
    <div className="min-h-[100dvh] bg-[var(--canvas)] text-[var(--text)]">
      <aside className="fixed inset-y-0 left-0 hidden w-56 border-r border-[var(--border)] bg-[var(--surface)] lg:flex lg:flex-col">
        <div className="flex h-16 items-center gap-3 border-b border-[var(--border)] px-5">
          <span className="grid size-8 place-items-center rounded-lg bg-[var(--text)] text-[var(--surface)]">
            <Pulse size={18} weight="bold" aria-hidden="true" />
          </span>
          <div>
            <p className="text-sm font-semibold tracking-[-0.02em]">Uptime Monitor</p>
            <p className="text-[11px] text-[var(--text-muted)]">Operations console</p>
          </div>
        </div>

        <nav className="flex-1 px-3 py-5" aria-label="Primary navigation">
          <ul className="space-y-1">
            {navigation.map(({ label, to, icon: Icon, end }) => (
              <li key={to}>
                <NavLink
                  to={to}
                  end={end}
                  className={({ isActive }) =>
                    `interactive flex h-9 items-center gap-3 rounded-lg px-3 text-sm font-medium ${
                      isActive
                        ? "bg-[var(--surface-muted)] text-[var(--text)]"
                        : "text-[var(--text-muted)] hover:bg-[var(--surface-muted)] hover:text-[var(--text)]"
                    }`
                  }
                >
                  <Icon size={17} weight="bold" aria-hidden="true" />
                  {label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>

        <div className="border-t border-[var(--border)] p-3">
          <p className="px-3 pb-2 text-[11px] text-[var(--text-muted)]">Data source</p>
          <div className="flex items-center gap-2 rounded-lg px-3 py-2 text-xs font-medium">
            <span className="size-2 rounded-full bg-[var(--text-muted)]" aria-hidden="true" />
            Spring Boot API
          </div>
        </div>
      </aside>

      <div className="lg:pl-56">
        <header className="sticky top-0 z-20 flex h-16 items-center justify-between border-b border-[var(--border)] bg-[color:var(--canvas)]/95 px-4 backdrop-blur-md sm:px-6 lg:px-8">
          <div className="flex items-center gap-3">
            <span className="grid size-8 place-items-center rounded-lg bg-[var(--text)] text-[var(--surface)] lg:hidden">
              <Pulse size={18} weight="bold" aria-hidden="true" />
            </span>
            <div>
              <p className="text-sm font-semibold">{pageTitle(location.pathname)}</p>
              <p className="hidden text-xs text-[var(--text-muted)] sm:block">
                Availability, latency, and incident state
              </p>
            </div>
          </div>

          <Tooltip.Root>
            <Tooltip.Trigger asChild>
              <button
                type="button"
                onClick={toggleTheme}
                className="interactive grid size-9 place-items-center rounded-lg border border-[var(--border)] bg-[var(--surface)] text-[var(--text-muted)] hover:text-[var(--text)]"
                aria-label={`Switch to ${theme === "dark" ? "light" : "dark"} mode`}
              >
                {theme === "dark" ? <Sun size={17} weight="bold" /> : <Moon size={17} weight="bold" />}
              </button>
            </Tooltip.Trigger>
            <Tooltip.Portal>
              <Tooltip.Content
                sideOffset={8}
                className="z-50 rounded-md bg-[var(--text)] px-2 py-1 text-xs text-[var(--surface)]"
              >
                Toggle theme
              </Tooltip.Content>
            </Tooltip.Portal>
          </Tooltip.Root>
        </header>

        <main className="mx-auto w-full max-w-[1440px] px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
          <Outlet />
        </main>

        <nav className="fixed inset-x-0 bottom-0 z-20 border-t border-[var(--border)] bg-[var(--surface)] px-2 py-2 lg:hidden" aria-label="Mobile navigation">
          <ul className="grid grid-cols-3 gap-1">
            {navigation.map(({ label, to, icon: Icon, end }) => (
              <li key={to}>
                <NavLink
                  to={to}
                  end={end}
                  className={({ isActive }) =>
                    `flex min-h-11 flex-col items-center justify-center gap-1 rounded-lg text-[11px] font-medium ${
                      isActive ? "bg-[var(--surface-muted)] text-[var(--text)]" : "text-[var(--text-muted)]"
                    }`
                  }
                >
                  <Icon size={17} weight="bold" aria-hidden="true" />
                  {label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
      </div>
    </div>
  );
}
