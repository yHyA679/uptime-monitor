export function formatDateTime(value: string | null | undefined): string {
  if (!value) return "Not checked yet";

  return new Intl.DateTimeFormat(undefined, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

export function formatRelativeTime(value: string | null | undefined): string {
  if (!value) return "Never";

  const seconds = Math.round((new Date(value).getTime() - Date.now()) / 1000);
  const formatter = new Intl.RelativeTimeFormat(undefined, { numeric: "auto" });
  const ranges: Array<[Intl.RelativeTimeFormatUnit, number]> = [
    ["year", 31_536_000],
    ["month", 2_592_000],
    ["day", 86_400],
    ["hour", 3_600],
    ["minute", 60],
  ];

  for (const [unit, range] of ranges) {
    if (Math.abs(seconds) >= range) {
      return formatter.format(Math.round(seconds / range), unit);
    }
  }

  return formatter.format(seconds, "second");
}

export function formatDuration(seconds: number | null): string {
  if (seconds === null) return "Ongoing";
  if (seconds < 60) return `${seconds}s`;

  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  if (minutes < 60) return `${minutes}m ${remainingSeconds}s`;

  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;
  return `${hours}h ${remainingMinutes}m`;
}

export function formatPercentage(value: number | null | undefined): string {
  if (value === null || value === undefined) return "No data";
  return `${value.toFixed(value === 100 ? 0 : 2)}%`;
}

export function formatResponseTime(value: number | null | undefined): string {
  if (value === null || value === undefined) return "No data";
  return `${Math.round(value)} ms`;
}

export function hostname(url: string): string {
  try {
    return new URL(url).hostname;
  } catch {
    return url;
  }
}
