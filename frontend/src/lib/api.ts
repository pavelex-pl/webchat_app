export class ApiError extends Error {
  status: number;
  detail: string;
  constructor(status: number, detail: string) {
    super(detail || `HTTP ${status}`);
    this.status = status;
    this.detail = detail;
  }
}

const NO_REFRESH_PATHS = [
  "/api/auth/login",
  "/api/auth/register",
  "/api/auth/refresh",
  "/api/auth/password-reset/request",
  "/api/auth/password-reset/confirm",
];

async function request<T>(path: string, init: RequestInit, retry = true): Promise<T> {
  const isFormData = typeof FormData !== "undefined" && init.body instanceof FormData;
  const baseHeaders: Record<string, string> = { Accept: "application/json" };
  if (!isFormData && init.body != null) baseHeaders["Content-Type"] = "application/json";

  const resp = await fetch(path, {
    ...init,
    credentials: "include",
    headers: { ...baseHeaders, ...(init.headers || {}) },
  });

  if (resp.status === 401 && retry && !NO_REFRESH_PATHS.some((p) => path.startsWith(p))) {
    const refreshed = await fetch("/api/auth/refresh", { method: "POST", credentials: "include" });
    if (refreshed.ok) return request<T>(path, init, false);
  }

  if (!resp.ok) {
    let detail = resp.statusText;
    try {
      const body = await resp.json();
      if (body?.detail) detail = body.detail;
    } catch {
      // ignore
    }
    throw new ApiError(resp.status, detail);
  }

  if (resp.status === 204 || resp.headers.get("Content-Length") === "0") {
    return undefined as T;
  }
  return (await resp.json()) as T;
}

export const api = {
  get: <T>(path: string) => request<T>(path, { method: "GET" }),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: "POST", body: body === undefined ? undefined : JSON.stringify(body) }),
  patch: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: "PATCH", body: body === undefined ? undefined : JSON.stringify(body) }),
  delete: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: "DELETE", body: body === undefined ? undefined : JSON.stringify(body) }),
  upload: <T>(path: string, file: File) => {
    const fd = new FormData();
    fd.append("file", file);
    return request<T>(path, { method: "POST", body: fd });
  },
};
