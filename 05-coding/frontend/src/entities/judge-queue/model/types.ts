/**
 * Judge queue monitoring (F4-10, F4-11). Covers the go-judge worker cluster, the jobs waiting on it
 * and the infrastructure events it emits.
 *
 * Boundary: infrastructure/service events live HERE, not in the system log. `admin_system_log`
 * carries only administrative actions taken by a human (F1-14) —
 * 02-bd/screens/admin/admin_system_log.md section 0 calls that a hard boundary.
 *
 * Only TWO priority levels exist. The mockup still carries a third, "Chấm lại", plus a matching
 * rejudge latency queue; rejudge is out of scope entirely (DEC-2026-0828-remove-rejudge-scope), and
 * DEC-2026-0831-judge-orchestration-ops-details confirms the remaining pair.
 */
export type JobPriority = "normal" | "high";

export type JobStatus = "running" | "waiting" | "sandboxError";

export type QueueJob = {
  id: string;
  submissionId: string;
  language: string;
  priority: JobPriority;
  status: JobStatus;
  /** Seconds waited. -1 means "not applicable" (the mockup prints an em dash for failed jobs). */
  waitSeconds: number;
  waitLabel: string;
};

export type WorkerStatus = "running" | "idle" | "overloaded";

export type Worker = {
  id: string;
  status: WorkerStatus;
  /** Load percentage, 0-100. */
  load: number;
  jobs: number;
  /** Uptime and vCPU, e.g. "Uptime 12 ngày · vCPU 4". */
  meta: string;
};

export type ClusterKpi = {
  key: string;
  value: string;
  colorVar: string;
};

/** Cluster controls that actually act on the queue (F4-10 includes operating what it monitors). */
export type ClusterControls = {
  paused: boolean;
  autoscale: boolean;
  highPriorityFirst: boolean;
};

export type QueueLatency = {
  priority: JobPriority;
  label: string;
  /** Bar fill, 0-100. */
  percent: number;
};

export type InfraLevel = "error" | "warn" | "info";

export type InfraEvent = {
  id: string;
  time: string;
  level: InfraLevel;
  message: string;
  service: string;
};

export type QueuePage = {
  kpis: ClusterKpi[];
  workers: Worker[];
  controls: ClusterControls;
  latency: QueueLatency[];
  jobs: QueueJob[];
  infraEvents: InfraEvent[];
  workerCount: number;
  maxWorkers: number;
};
