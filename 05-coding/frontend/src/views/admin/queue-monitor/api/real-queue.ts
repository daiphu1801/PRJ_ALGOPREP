import { api } from "@/shared/api";
export type QueueSnapshot = {
  generatedAt: string;
  status: "PAUSED" | "BACKLOGGED" | "NORMAL";
  pendingCount: number;
  queueConsumptionPaused: boolean;
  autoscaleEnabled: boolean;
  autoscaleJobThreshold: number;
  avgWaitMillis: number | null;
  latencyCapSeconds: number;
  workerAutoscaleMax: number;
};
export type QueueSettings = {
  autoscaleJobThreshold: number;
  jobsPerPage: number;
  errorRetentionHours: number;
  latencyCapSeconds: number;
  updatedAt: string;
};
export type QueueJobs = {
  items: {
    submissionId: string;
    language: string;
    status: string;
    waitingMillis: number | null;
  }[];
  page: number;
  pageSize: number;
  totalCount: number;
  counts: { waiting: number; failed: number; all: number };
};
export async function readQueue() {
  const [snapshot, settings, infra] = await Promise.all([
    api.get<QueueSnapshot>("/judge/admin/queue/snapshot"),
    api.get<QueueSettings>("/judge/admin/queue/settings"),
    api.get<{ warnCount: number; errorCount: number }>(
      "/judge/admin/queue/infra-summary",
    ),
  ]);
  return { snapshot, settings, infra };
}
export function readJobs(tab: string, page: number) {
  return api.get<QueueJobs>("/judge/admin/queue/jobs", { tab, page });
}
export async function setQueueControl(
  key: "paused" | "autoscale",
  value: boolean,
  expected: boolean,
) {
  return key === "paused"
    ? api.put("/judge/admin/queue/consumption", {
        paused: value,
        expectedPaused: expected,
      })
    : api.put("/judge/admin/queue/autoscale", {
        enabled: value,
        expectedEnabled: expected,
      });
}
export function saveQueueSettings(values: Omit<QueueSettings, "updatedAt">) {
  return api.put<QueueSettings>("/judge/admin/queue/settings", values);
}
