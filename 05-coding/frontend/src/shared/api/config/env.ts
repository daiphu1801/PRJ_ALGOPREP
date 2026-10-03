// Client-side env vars — only vars prefixed NEXT_PUBLIC_ are exposed to the browser.
export const env = {
  apiBaseUrl: process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api",
  wsBaseUrl: process.env.NEXT_PUBLIC_WS_BASE_URL ?? "ws://localhost:8080/ws",
  // PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
  // true: every entity api reads from entities/<x>/api/__mock__/ instead of calling the real backend.
  mockData: process.env.NEXT_PUBLIC_MOCK_DATA === "true",
} as const;
