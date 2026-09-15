import "@testing-library/jest-dom/vitest";

// Vitest does not load .env.local (only `next dev`/`next build` do) — mirror its
// NEXT_PUBLIC_MOCK_DATA=true here so entities/*/api tests exercise the __mock__ path instead of
// hitting shared/api/client.ts's notImplemented() stub against a backend that doesn't exist yet.
process.env.NEXT_PUBLIC_MOCK_DATA = "true";
