import type {
  CreateWebsiteInput,
  Incident,
  MonitoringResult,
  Website,
  WebsiteSettingsInput,
  WebsiteStats,
} from "../types/api";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options?.headers,
    },
  });

  if (!response.ok) {
    let message = `Request failed with status ${response.status}`;

    try {
      const body = (await response.json()) as { detail?: string; message?: string };
      message = body.detail ?? body.message ?? message;
    } catch {
      // Keep the HTTP status fallback when the response has no JSON body.
    }

    throw new ApiError(message, response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export const websiteApi = {
  list: () => request<Website[]>("/api/websites"),
  get: (id: number) => request<Website>(`/api/websites/${id}`),
  create: (input: CreateWebsiteInput) =>
    request<Website>("/api/websites", {
      method: "POST",
      body: JSON.stringify(input),
    }),
  remove: (id: number) =>
    request<void>(`/api/websites/${id}`, { method: "DELETE" }),
  check: (id: number) =>
    request<Website>(`/api/websites/${id}/check`, { method: "POST" }),
  history: (id: number) =>
    request<MonitoringResult[]>(`/api/websites/${id}/history`),
  stats: (id: number) => request<WebsiteStats>(`/api/websites/${id}/stats`),
  incidents: (id: number) =>
    request<Incident[]>(`/api/websites/${id}/incidents`),
  updateSettings: (id: number, input: WebsiteSettingsInput) =>
    request<Website>(`/api/websites/${id}`, {
      method: "PATCH",
      body: JSON.stringify(input),
    }),
};
