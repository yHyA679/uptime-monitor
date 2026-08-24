export type WebsiteStatus = "UP" | "DOWN" | null;
export type IncidentStatus = "OPEN" | "RESOLVED";

export interface Website {
  id: number;
  name: string;
  url: string;
  status: WebsiteStatus;
  checkIntervalSeconds: number;
  enabled: boolean;
}

export interface MonitoringResult {
  id: number;
  website: Website;
  status: Exclude<WebsiteStatus, null>;
  responseTime: number;
  checkedAt: string;
}

export interface Incident {
  id: number;
  website: Website;
  startedAt: string;
  resolvedAt: string | null;
  durationSeconds: number | null;
  status: IncidentStatus;
}

export interface WebsiteStats {
  websiteId: number;
  totalChecks: number;
  upChecks: number;
  downChecks: number;
  uptimePercentage: number;
  averageResponseTime: number;
  lastCheckedAt: string | null;
}

export interface CreateWebsiteInput {
  name: string;
  url: string;
  checkIntervalSeconds: number;
  enabled: boolean;
}

export interface WebsiteSettingsInput {
  checkIntervalSeconds?: number;
  enabled?: boolean;
}

export interface WebsiteOverview {
  website: Website;
  stats: WebsiteStats | null;
}
