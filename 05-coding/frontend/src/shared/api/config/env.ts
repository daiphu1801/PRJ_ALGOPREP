// Client-side env vars — only vars prefixed NEXT_PUBLIC_ are exposed to the browser.
export const env = {
  apiBaseUrl:
    process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1",
  wsBaseUrl: process.env.NEXT_PUBLIC_WS_BASE_URL ?? "ws://localhost:8080/ws",
  // Origin of presigned object URLs, allowed by the CSP in proxy.ts. The release build sets "/"
  // because its reverse proxy serves object storage on the application origin.
  objectStorageUrl:
    process.env.NEXT_PUBLIC_OBJECT_STORAGE_URL ?? "http://localhost:9000",
  // PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
  // true: every entity api reads from entities/<x>/api/__mock__/ instead of calling the real backend.
  mockData: process.env.NEXT_PUBLIC_MOCK_DATA === "true",
} as const;
