import axios, { type AxiosError } from "axios";
import { env } from "@/shared/config";
import { ApiError } from "./api-error";
import { tokenStore } from "./token-store";

const httpClient = axios.create({
  baseURL: env.apiBaseUrl,
  withCredentials: true, // gửi cookie Refresh Token HttpOnly
});

httpClient.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Chống lặp làm mới token vô hạn: một lần refresh thất bại thì đăng xuất, không thử lại.
let refreshInFlight: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  refreshInFlight ??= axios
    .post<{ accessToken: string }>(`${env.apiBaseUrl}/auth/refresh`, null, { withCredentials: true })
    .then((res) => res.data.accessToken)
    .catch(() => null)
    .finally(() => {
      refreshInFlight = null;
    });
  return refreshInFlight;
}

httpClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<{ code?: string }>) => {
    const original = error.config;
    if (error.response?.status === 401 && original && !("_retried" in original)) {
      const newToken = await refreshAccessToken();
      if (newToken) {
        tokenStore.set(newToken);
        Object.assign(original, { _retried: true });
        return httpClient.request(original);
      }
      tokenStore.set(null);
    }

    const code = error.response?.data?.code ?? "UNKNOWN_ERROR";
    const status = error.response?.status ?? 0;
    return Promise.reject(new ApiError(code, status, error.message));
  },
);

/** Envelope response chung của backend — đường đọc dùng data trực tiếp qua mapper của entity. */
export async function apiGet<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const res = await httpClient.get<T>(url, { params });
  return res.data;
}

export async function apiPost<T>(url: string, body?: unknown): Promise<T> {
  const res = await httpClient.post<T>(url, body);
  return res.data;
}

export const api = { get: apiGet, post: apiPost };
