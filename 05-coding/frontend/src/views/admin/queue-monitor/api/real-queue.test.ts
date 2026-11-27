import { beforeEach, expect, it, vi } from "vitest";
const get = vi.fn();
const put = vi.fn();
vi.mock("@/shared/api", () => ({
  api: {
    get: (...args: unknown[]) => get(...args),
    put: (...args: unknown[]) => put(...args),
  },
}));
import {
  readJobs,
  setQueueControl,
  saveQueueSettings,
  toJobsPage,
  toQueueSummary,
} from "./real-queue";
beforeEach(() => {
  get.mockReset();
  put.mockReset();
});
it("sends the observed state to detect a concurrent pause/autoscale change", async () => {
  await setQueueControl("paused", true, false);
  await setQueueControl("autoscale", false, true);
  expect(put.mock.calls).toEqual([
    ["/judge/admin/queue/consumption", { paused: true, expectedPaused: false }],
    ["/judge/admin/queue/autoscale", { enabled: false, expectedEnabled: true }],
  ]);
});
it("sends server tab and page without fetching all jobs for local filtering", async () => {
  await readJobs("failed", 3);
  expect(get).toHaveBeenCalledExactlyOnceWith("/judge/admin/queue/jobs", {
    tab: "failed",
    page: 3,
  });
});
it("updates only the four queue settings in the contract", async () => {
  const settings = {
    autoscaleJobThreshold: 21,
    jobsPerPage: 10,
    errorRetentionHours: 48,
    latencyCapSeconds: 15,
  };
  await saveQueueSettings(settings);
  expect(put).toHaveBeenCalledExactlyOnceWith(
    "/judge/admin/queue/settings",
    settings,
  );
});
it("maps the server snapshot to the shape the screen renders, with no workers", () => {
  const summary = toQueueSummary({
    snapshot: {
      generatedAt: "2026-11-14T10:00:00Z",
      status: "BACKLOGGED",
      pendingCount: 25,
      queueConsumptionPaused: false,
      autoscaleEnabled: true,
      autoscaleJobThreshold: 20,
      avgWaitMillis: 4400,
      latencyCapSeconds: 10,
      workerAutoscaleMax: 8,
    },
    settings: {
      autoscaleJobThreshold: 20,
      jobsPerPage: 15,
      errorRetentionHours: 24,
      latencyCapSeconds: 10,
      updatedAt: "",
    },
    infra: { warnCount: 2, errorCount: 1 },
  });
  expect(summary).toMatchObject({
    state: "backlog",
    pendingCount: 25,
    latency: { seconds: 4, label: "4s" },
    settings: { autoscaleThreshold: 20, jobsPerPage: 15 },
    workers: [],
    infra: { warn: 2, error: 1 },
  });
  expect(summary.generatedAt).toBe(Date.parse("2026-11-14T10:00:00Z"));
});
const baseSnapshot = {
  generatedAt: "2026-11-22T11:25:00Z",
  status: "BACKLOGGED" as const,
  pendingCount: 22,
  queueConsumptionPaused: false,
  autoscaleEnabled: true,
  autoscaleJobThreshold: 20,
  avgWaitMillis: 27000,
  latencyCapSeconds: 10,
  workerAutoscaleMax: 8,
};
const grading = (snapshot: Parameters<typeof toQueueSummary>[0]["snapshot"]) =>
  toQueueSummary({
    snapshot,
    settings: {
      autoscaleJobThreshold: 20,
      jobsPerPage: 20,
      errorRetentionHours: 24,
      latencyCapSeconds: 10,
      updatedAt: "",
    },
    infra: { warnCount: 0, errorCount: 0 },
  }).grading;
it("maps the reported consumers, the cap and the last scaling event into the grading block", () => {
  expect(
    grading({
      ...baseSnapshot,
      finishedLastMinute: 31,
      estimatedClearSeconds: 42,
      consumers: {
        target: 5,
        active: 4,
        reportedAt: "2026-11-22T11:24:58Z",
        lastScaling: {
          at: "2026-11-22T11:24:28Z",
          from: 4,
          to: 5,
          reason: "BACKLOG",
          pendingCount: 22,
        },
      },
    }),
  ).toEqual({
    consumers: {
      target: 5,
      active: 4,
      max: 8,
      lastScaling: {
        at: Date.parse("2026-11-22T11:24:28Z"),
        from: 4,
        to: 5,
        reason: "BACKLOG",
        pendingCount: 22,
      },
    },
    finishedLastMinute: 31,
    estimatedClearSeconds: 42,
  });
});
it("keeps consumers null when the worker has not reported, and never invents numbers", () => {
  expect(
    grading({
      ...baseSnapshot,
      finishedLastMinute: 12,
      estimatedClearSeconds: null,
      consumers: null,
    }),
  ).toEqual({
    consumers: null,
    finishedLastMinute: 12,
    estimatedClearSeconds: null,
  });
  // An older server without the new fields yields zero throughput and no estimate, not undefined.
  expect(grading(baseSnapshot)).toEqual({
    consumers: null,
    finishedLastMinute: 0,
    estimatedClearSeconds: null,
  });
});
it("passes a worker that never scaled as consumers with no last scaling", () => {
  expect(
    grading({
      ...baseSnapshot,
      consumers: {
        target: 1,
        active: 1,
        reportedAt: "2026-11-22T11:24:58Z",
        lastScaling: null,
      },
    })?.consumers,
  ).toEqual({ target: 1, active: 1, max: 8, lastScaling: null });
});
it("maps server jobs: PENDING is waiting, SYSTEM_ERROR is a sandbox error, a missing wait is a dash", () => {
  const page = toJobsPage({
    items: [
      {
        submissionId: "a",
        language: "PYTHON",
        status: "PENDING",
        waitingMillis: 3000,
      },
      {
        submissionId: "b",
        language: "JAVA",
        status: "SYSTEM_ERROR",
        waitingMillis: null,
      },
    ],
    page: 1,
    pageSize: 20,
    totalCount: 2,
    counts: { waiting: 1, failed: 1, all: 2 },
  });
  expect(page.items.map((job) => [job.status, job.waitLabel])).toEqual([
    ["waiting", "3s"],
    ["sandboxError", "—"],
  ]);
  expect(page).toMatchObject({ total: 2, sortable: false });
});
