// PROTOTYPE mock — no backend endpoint exists yet, and nothing here is realtime (the real screen
// reads a STOMP topic per BD section 7).
// Values from 09-layoutBase/Admin - Hàng đợi chấm.dc.html:440-530, with every rejudge remnant
// removed (DEC-2026-0828-remove-rejudge-scope):
//   - the "Hàng đợi chấm lại" latency row is gone, and so is job priority altogether (BD ADM0401 Q4):
//     latency is one average-wait figure.
//   - infrastructure events live in entities/infra-event now.
import { countRecentInfraAlerts } from "@/entities/infra-event";
import { createSettingsStore } from "@/shared/lib/settings-store";
import { queueSettings } from "../../model/settings";
import type {
  ClusterControls,
  JobsPage,
  QueueJob,
  QueueLatency,
  QueuePage,
  QueueState,
  QueueSummary,
  QueueTab,
  Worker,
} from "../../model/types";

// dc.html:452-457.
const WORKERS: Worker[] = [
  {
    id: "go-judge-w1",
    status: "running",
    load: 78,
    jobs: 3,
    meta: "Uptime 12 ngày · vCPU 4",
  },
  {
    id: "go-judge-w2",
    status: "running",
    load: 64,
    jobs: 2,
    meta: "Uptime 12 ngày · vCPU 4",
  },
  {
    id: "go-judge-w3",
    status: "overloaded",
    load: 93,
    jobs: 1,
    meta: "Uptime 6 giờ · vCPU 2",
  },
  {
    // Busy like the rest: an idle worker beside a backed-up queue read as a bug (admin UX review F14).
    id: "go-judge-w4",
    status: "running",
    load: 41,
    jobs: 1,
    meta: "Uptime 3 ngày · vCPU 4",
  },
];

// dc.html:471-475 (`state.toggles`).
const CONTROLS: ClusterControls = {
  paused: false,
  autoscale: true,
};

// dc.html:506-512.
const JOBS: QueueJob[] = [
  {
    id: "#J-77410",
    submissionId: "#SB-90412",
    language: "Python 3",
    status: "running",
    waitSeconds: 0.4,
    waitLabel: "0,4s",
  },
  {
    id: "#J-77409",
    submissionId: "#SB-90411",
    language: "C++ 17",
    status: "running",
    waitSeconds: 0.9,
    waitLabel: "0,9s",
  },
  {
    id: "#J-77406",
    submissionId: "#SB-90408",
    language: "Java 21",
    status: "waiting",
    waitSeconds: 12,
    waitLabel: "12s",
  },
  {
    id: "#J-77404",
    submissionId: "#SB-90406",
    language: "Python 3",
    status: "waiting",
    waitSeconds: 31,
    waitLabel: "31s",
  },
  {
    id: "#J-77401",
    submissionId: "#SB-90403",
    language: "C++ 17",
    status: "waiting",
    waitSeconds: 46,
    waitLabel: "46s",
  },
  {
    id: "#J-77398",
    submissionId: "#SB-90399",
    language: "Java 21",
    status: "sandboxError",
    waitSeconds: -1,
    waitLabel: "—",
  },
];

// Extra waiting jobs so the table spans more than one page at the default 20 rows.
const MORE_JOBS: QueueJob[] = Array.from({ length: 18 }, (_, index) => {
  const seconds = 2 + index * 2;
  const languages = ["Python 3", "C++ 17", "Java 21"];
  return {
    id: `#J-774${20 + index}`,
    submissionId: `#SB-904${20 + index}`,
    language: languages[index % 3]!,
    status: "waiting" as const,
    waitSeconds: seconds,
    waitLabel: `${seconds}s`,
  };
});

// Average wait of the jobs on screen (sandbox errors have no wait), not a fixed figure that
// disagreed with the list (admin UX review F14).
function averageWait(jobs: QueueJob[]): QueueLatency {
  const waits = jobs.map((job) => job.waitSeconds).filter((s) => s >= 0);
  const seconds =
    Math.round((waits.reduce((sum, s) => sum + s, 0) / waits.length) * 10) / 10;
  return { seconds, label: `${seconds.toLocaleString("vi-VN")}s` };
}

export function fetchQueuePage(): QueuePage {
  const jobs = [...JOBS, ...MORE_JOBS];
  return {
    workers: WORKERS.map((worker) => ({ ...worker })),
    controls: { ...CONTROLS },
    latency: averageWait(jobs),
    jobs: jobs.map((job) => ({ ...job })),
    workerCount: 4,
    maxWorkers: 8,
    generatedAt: Date.now(),
  };
}

/** The two cluster switches, kept in memory so a toggle survives a refresh within the session. */
export const mockControls = createSettingsStore<ClusterControls>({
  ...CONTROLS,
});

export function fetchQueueSummary(): QueueSummary {
  const page = fetchQueuePage();
  const controls = mockControls.get();
  const settings = queueSettings.get();
  const waiting = page.jobs.filter((job) => job.status === "waiting").length;
  // Priority: paused > an overloaded worker > backlog at or above the autoscale threshold.
  let state: QueueState = "ok";
  if (controls.paused) state = "paused";
  else if (page.workers.some((worker) => worker.status === "overloaded"))
    state = "overloaded";
  else if (waiting >= settings.autoscaleThreshold) state = "backlog";
  return {
    state,
    pendingCount: waiting,
    controls,
    latency: page.latency,
    settings,
    workers: page.workers,
    workerCount: page.workerCount,
    maxWorkers: page.maxWorkers,
    generatedAt: page.generatedAt,
    infra: countRecentInfraAlerts(),
  };
}

/** Client-side tab, sort and paging over the fixture, like the server would do it. */
export function fetchQueueJobs(
  tab: QueueTab,
  pageNumber: number,
  direction: "asc" | "desc",
): JobsPage {
  const all = fetchQueuePage().jobs;
  const counts = {
    waiting: all.filter((job) => job.status === "waiting").length,
    failed: all.filter((job) => job.status === "sandboxError").length,
    all: all.length,
  };
  const factor = direction === "asc" ? 1 : -1;
  // Failed jobs always sink to the bottom: they carry waitSeconds -1, so a plain numeric sort
  // put them first when ascending (BD ADM0401 F NO10, UX review B3).
  const visible = all
    .filter((job) =>
      tab === "waiting"
        ? job.status === "waiting"
        : tab === "failed"
          ? job.status === "sandboxError"
          : true,
    )
    .sort((a, b) => {
      const aFailed = a.status === "sandboxError";
      const bFailed = b.status === "sandboxError";
      if (aFailed !== bFailed) return aFailed ? 1 : -1;
      return (a.waitSeconds - b.waitSeconds) * factor;
    });
  const { jobsPerPage } = queueSettings.get();
  const lastPage = Math.max(1, Math.ceil(visible.length / jobsPerPage));
  const page = Math.min(pageNumber, lastPage);
  return {
    items: visible.slice((page - 1) * jobsPerPage, page * jobsPerPage),
    page,
    pageSize: jobsPerPage,
    total: visible.length,
    counts,
    sortable: true,
  };
}
