import axios, { type AxiosError } from "axios";
import { env } from "@/shared/api/config";
import { type ErrorBody, toApiError } from "./api-error";
import { tokenStore } from "./token-store";

const httpClient = axios.create({
  baseURL: env.apiBaseUrl,
  withCredentials: true, // send the HttpOnly Refresh Token cookie
});

httpClient.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Prevent infinite token-refresh loops: a refresh the server refuses logs the user out, no retry.
// `ended` is true only for that refusal (401 or 403); a 5xx or a dropped connection says nothing
// about the session, so the user stays signed in and the failed call simply fails.
type RefreshResult = { token: string | null; ended: boolean };
let refreshInFlight: Promise<RefreshResult> | null = null;

async function refreshSession(): Promise<RefreshResult> {
  refreshInFlight ??= axios
    .post<{ accessToken: string }>(
      `${env.apiBaseUrl.replace(/\/$/, "")}/identity/auth/refresh`,
      null,
      {
        withCredentials: true,
        headers: { "X-Requested-With": "XMLHttpRequest" },
      },
    )
    .then((res) => {
      const token = res.data.accessToken;
      return typeof token === "string" && token.length > 0
        ? { token, ended: false }
        : { token: null, ended: true };
    })
    .catch((error: unknown) => {
      const status = axios.isAxiosError(error) ? error.response?.status : 0;
      return { token: null, ended: status === 401 || status === 403 };
    })
    .finally(() => {
      refreshInFlight = null;
    });
  return refreshInFlight;
}

export async function refreshAccessToken(): Promise<string | null> {
  return (await refreshSession()).token;
}

const AUTH_PATH = "/identity/auth/";

function isAuthEndpoint(url: string | undefined): boolean {
  return url?.includes(AUTH_PATH) ?? false;
}

httpClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ErrorBody>) => {
    const original = error.config;
    // A 401 from an auth endpoint (wrong password, bad code, refresh itself) is the answer, not an
    // expired session, so it must not trigger a refresh.
    if (
      error.response?.status === 401 &&
      original &&
      !("_retried" in original) &&
      !isAuthEndpoint(original.url)
    ) {
      const { token: newToken, ended } = await refreshSession();
      if (newToken) {
        tokenStore.set(newToken);
        Object.assign(original, { _retried: true });
        return httpClient.request(original);
      }
      if (ended) tokenStore.set(null);
    }

    return Promise.reject(
      toApiError(
        error.response?.status ?? 0,
        await readErrorBody(error.response?.data),
        error.message,
      ),
    );
  },
);

/** A `responseType: "blob"` call receives its error body as a Blob too; read it back as JSON. */
async function readErrorBody(data: unknown): Promise<ErrorBody | undefined> {
  if (!(data instanceof Blob)) return data as ErrorBody | undefined;
  // FileReader rather than `blob.text()`, which jsdom (the test environment) does not have.
  const text = await new Promise<string>((resolve) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => resolve("");
    reader.readAsText(data);
  });
  try {
    return JSON.parse(text) as ErrorBody;
  } catch {
    return undefined;
  }
}

/** Common backend response envelope — the read path uses `data` directly via the entity's mapper. */
export async function apiGet<T>(
  url: string,
  params?: Record<string, unknown>,
): Promise<T> {
  const res = await httpClient.get<T>(url, { params });
  return res.data;
}

export async function apiPost<T>(url: string, body?: unknown): Promise<T> {
  const res = await httpClient.post<T>(url, body);
  return res.data;
}

export async function apiPut<T>(url: string, body?: unknown): Promise<T> {
  const res = await httpClient.put<T>(url, body);
  return res.data;
}

export async function apiPatch<T>(url: string, body?: unknown): Promise<T> {
  const res = await httpClient.patch<T>(url, body);
  return res.data;
}

export async function apiDelete<T = void>(url: string): Promise<T> {
  const res = await httpClient.delete<T>(url);
  return res.data;
}

/** GET a file as a Blob (CSV export); the Bearer token and the 401 refresh work as for any call. */
export async function apiDownload(
  url: string,
  options: {
    params?: Record<string, unknown>;
    headers?: Record<string, string>;
  } = {},
): Promise<Blob> {
  return (await apiDownloadFile(url, options)).blob;
}

/** Same call as `apiDownload`, but also lets the caller read a response header (name is case-insensitive). */
export async function apiDownloadFile(
  url: string,
  options: {
    params?: Record<string, unknown>;
    headers?: Record<string, string>;
  } = {},
): Promise<{ blob: Blob; header: (name: string) => string | null }> {
  const res = await httpClient.get<Blob>(url, {
    ...options,
    responseType: "blob",
    // The default serializer joins an array as `columns[]=a`; the export endpoint reads `columns=a&columns=b`.
    paramsSerializer: { indexes: null },
  });
  return {
    blob: res.data,
    header: (name) => {
      const value = res.headers?.[name.toLowerCase()];
      return value == null ? null : String(value);
    },
  };
}

/** POST that answers with a file (CSV export of a filtered set, sent as a body); same auth and error handling. */
export async function apiDownloadPost(
  url: string,
  body?: unknown,
): Promise<{ blob: Blob; filename: string | null }> {
  const res = await httpClient.post<Blob>(url, body, { responseType: "blob" });
  const disposition = String(res.headers?.["content-disposition"] ?? "");
  const match = /filename="?([^";]+)"?/i.exec(disposition);
  return { blob: res.data, filename: match?.[1] ?? null };
}

export const api = {
  get: apiGet,
  download: apiDownload,
  downloadFile: apiDownloadFile,
  downloadPost: apiDownloadPost,
  post: apiPost,
  put: apiPut,
  patch: apiPatch,
  delete: apiDelete,
};
