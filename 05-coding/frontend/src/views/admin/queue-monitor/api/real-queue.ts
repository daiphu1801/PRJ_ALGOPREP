import { api } from "@/shared/api";
import type {
  GradingBlock,
  JobStatus,
  JobsPage,
  QueueState,
  QueueSettings as QueueSettingsModel,
  QueueSummary,
  ScaleReason,
} from "../model/types";
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
  finishedLastMinute?: number;
  estimatedClearSeconds?: number | null;
  consumers?: {
    target: number;
    active: number;
    reportedAt: string;
    lastScaling: {
      at: string;
      from: number;
      to: number;
      reason: ScaleReason;
      pendingCount: number;
    } | null;
  } | null;
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
export function readQueue() {
  return api.get<QueueSnapshot>("/judge/admin/queue/snapshot");
}
export async function readQueueSettings(): Promise<QueueSettingsModel> {
  const {
    autoscaleJobThreshold,
    jobsPerPage,
    errorRetentionHours,
    latencyCapSeconds,
  } = await api.get<QueueSettings>("/judge/admin/queue/settings");
  return {
    autoscaleThreshold: autoscaleJobThreshold,
    jobsPerPage,
    errorRetentionHours,
    latencyCapSeconds,
  };
}
export async function readQueueInfra() {
  const infra = await api.get<InfraSummary>("/judge/admin/queue/infra-summary");
  return { warn: infra.warnCount, error: infra.errorCount };
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

type InfraSummary = { warnCount: number; errorCount: number };

const STATE: Record<QueueSnapshot["status"], QueueState> = {
  PAUSED: "paused",
  BACKLOGGED: "backlog",
  NORMAL: "ok",
};

/** Fields the live STOMP batch also carries, so it can refresh them without refetching the settings. */
export function summaryFromSnapshot(
  snapshot: QueueSnapshot,
  infra: InfraSummary | null,
): Pick<
  QueueSummary,
  | "state"
  | "pendingCount"
  | "controls"
  | "latency"
  | "generatedAt"
  | "infra"
  | "grading"
> {
  const seconds =
    snapshot.avgWaitMillis == null
      ? null
      : Math.round(snapshot.avgWaitMillis / 1000);
  return {
    state: STATE[snapshot.status],
    pendingCount: snapshot.pendingCount,
    controls: {
      paused: snapshot.queueConsumptionPaused,
      autoscale: snapshot.autoscaleEnabled,
    },
    latency: { seconds, label: seconds == null ? "—" : `${seconds}s` },
    generatedAt: Date.parse(snapshot.generatedAt),
    infra: infra && { warn: infra.warnCount, error: infra.errorCount },
    grading: gradingFrom(snapshot),
  };
}

/** Never carries the previous batch over: a missing report is `consumers: null`, not the last numbers. */
function gradingFrom(snapshot: QueueSnapshot): GradingBlock {
  const reported = snapshot.consumers;
  const last = reported?.lastScaling;
  return {
    consumers: reported
      ? {
          target: reported.target,
          active: reported.active,
          max: snapshot.workerAutoscaleMax,
          lastScaling: last
            ? {
                at: Date.parse(last.at),
                from: last.from,
                to: last.to,
                reason: last.reason,
                pendingCount: last.pendingCount,
              }
            : null,
        }
      : null,
    finishedLastMinute: snapshot.finishedLastMinute ?? 0,
    estimatedClearSeconds: snapshot.estimatedClearSeconds ?? null,
  };
}

export function toQueueSummary(data: {
  snapshot: QueueSnapshot;
  settings: QueueSettings | null;
  infra: InfraSummary | null;
}): QueueSummary {
  const { snapshot, settings } = data;
  return {
    ...summaryFromSnapshot(snapshot, data.infra),
    settings: settings && {
      autoscaleThreshold: settings.autoscaleJobThreshold,
      jobsPerPage: settings.jobsPerPage,
      errorRetentionHours: settings.errorRetentionHours,
      latencyCapSeconds: settings.latencyCapSeconds,
    },
    workers: [],
    workerCount: 0,
    maxWorkers: snapshot.workerAutoscaleMax,
  };
}

const JOB_STATUS: Record<string, JobStatus> = {
  PENDING: "waiting",
  SYSTEM_ERROR: "sandboxError",
};

export function toJobsPage(jobs: QueueJobs): JobsPage {
  return {
    items: jobs.items.map((row) => {
      const seconds =
        row.waitingMillis == null ? -1 : Math.round(row.waitingMillis / 1000);
      return {
        id: row.submissionId,
        submissionId: row.submissionId,
        language: row.language,
        status: JOB_STATUS[row.status] ?? "running",
        waitSeconds: seconds,
        waitLabel: seconds < 0 ? "—" : `${seconds}s`,
      };
    }),
    page: jobs.page,
    pageSize: jobs.pageSize,
    total: jobs.totalCount,
    counts: jobs.counts,
    sortable: false,
  };
}
