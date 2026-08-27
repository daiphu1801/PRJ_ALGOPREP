// Biến môi trường phía client — chỉ những biến bắt đầu NEXT_PUBLIC_ mới lộ ra browser.
export const env = {
  apiBaseUrl: process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api",
  wsBaseUrl: process.env.NEXT_PUBLIC_WS_BASE_URL ?? "ws://localhost:8080/ws",
} as const;
