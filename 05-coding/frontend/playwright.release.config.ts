import { defineConfig } from "@playwright/test";

if (process.env.E2E_BASE_URL !== "https://localhost:3443") {
  throw new Error("Release smoke requires the isolated HTTPS Compose stack.");
}

export default defineConfig({
  testDir: "./e2e",
  testMatch: [
    "**/auth-real-signup-login.spec.ts",
    "**/problem-authoring-real.spec.ts",
    "**/release-smoke.spec.ts",
  ],
  workers: 1,
  forbidOnly: true,
  reporter: [["list"], ["json", { outputFile: "test-results/release.json" }]],
  use: {
    baseURL: process.env.E2E_BASE_URL,
    // The local proxy uses an isolated CA. Its certificate is checked separately with curl.
    ignoreHTTPSErrors: true,
    trace: "retain-on-failure",
  },
});
