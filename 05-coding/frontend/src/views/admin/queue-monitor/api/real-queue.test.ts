import { beforeEach, expect, it, vi } from "vitest";
const get = vi.fn(); const put = vi.fn();
vi.mock("@/shared/api", () => ({ api: { get: (...args: unknown[]) => get(...args), put: (...args: unknown[]) => put(...args) } }));
import { readJobs, setQueueControl, saveQueueSettings } from "./real-queue";
beforeEach(() => { get.mockReset(); put.mockReset(); });
it("sends the observed state to detect a concurrent pause/autoscale change", async () => {
  await setQueueControl("paused", true, false);
  await setQueueControl("autoscale", false, true);
  expect(put.mock.calls).toEqual([["/judge/admin/queue/consumption", { paused: true, expectedPaused: false }], ["/judge/admin/queue/autoscale", { enabled: false, expectedEnabled: true }]]);
});
it("sends server tab and page without fetching all jobs for local filtering", async () => {
  await readJobs("failed", 3);
  expect(get).toHaveBeenCalledExactlyOnceWith("/judge/admin/queue/jobs", { tab: "failed", page: 3 });
});
it("updates only the four queue settings in the contract", async () => {
  const settings = { autoscaleJobThreshold: 21, jobsPerPage: 10, errorRetentionHours: 48, latencyCapSeconds: 15 };
  await saveQueueSettings(settings);
  expect(put).toHaveBeenCalledExactlyOnceWith("/judge/admin/queue/settings", settings);
});
