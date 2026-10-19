// PROTOTYPE mock — no backend endpoint exists yet, and nothing here is realtime (the real screen
// reads a STOMP topic per BD section 7).
// Values from 09-layoutBase/Admin - Hàng đợi chấm.dc.html:440-530, with every rejudge remnant
// removed (DEC-2026-0828-remove-rejudge-scope):
//   - the "Hàng đợi chấm lại" latency row is gone, and so is job priority altogether (BD ADM0401 Q4):
//     latency is one average-wait figure.
//   - infrastructure events live in entities/infra-event now.
import type {
  ClusterControls,
  QueueJob,
  QueueLatency,
  QueuePage,
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
