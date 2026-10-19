/**
 * Judge queue monitoring (F4-10, F4-11). Covers the go-judge worker cluster, the jobs waiting on it
 * and the two cluster controls.
 *
 * Scope notes:
 * - Infrastructure events moved to their own screen (`admin_infra_events`, ADM0404, entity
 *   `infra-event`); this screen only links to it.
 * - There is no job priority: no source table defines one (BD ADM0401 Q4), and rejudge is out of
 *   scope (DEC-2026-0828-remove-rejudge-scope). Latency is a single average-wait figure.
 */
export type JobStatus = "running" | "waiting" | "sandboxError";

export type QueueJob = {
  id: string;
  submissionId: string;
  language: string;
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

/** Cluster controls that actually act on the queue (F4-10 includes operating what it monitors). */
export type ClusterControls = {
  paused: boolean;
  autoscale: boolean;
};

/** Average wait from submit to a worker picking the job up. */
export type QueueLatency = {
  seconds: number;
  label: string;
};

export type QueuePage = {
  workers: Worker[];
  controls: ClusterControls;
  latency: QueueLatency;
  jobs: QueueJob[];
  workerCount: number;
  maxWorkers: number;
  /** Epoch ms the snapshot was taken, for "Cập nhật lúc" and the stale check. */
  generatedAt: number;
};
