// PROTOTYPE mock — no backend endpoint exists yet, and nothing here is realtime (the real screen
// reads a STOMP topic per BD section 7).
// Values from 09-layoutBase/Admin - Hàng đợi chấm.dc.html:440-530, with every rejudge remnant
// removed (DEC-2026-0828-remove-rejudge-scope):
//   - the "Hàng đợi chấm lại" latency row is gone (2 queues, not 3),
//   - the "Chấm lại" priority is gone (2 levels, not 3); the two jobs that carried it are now
//     normal priority, which is what they would be once rejudge no longer exists.
import type {
  ClusterControls,
  ClusterKpi,
  InfraEvent,
  QueueJob,
  QueueLatency,
  QueuePage,
  Worker,
} from "../../model/types";

// dc.html:440-445.
const KPIS: ClusterKpi[] = [
  { key: "waiting", value: "14", colorVar: "--color-admin-teal" },
  { key: "running", value: "6", colorVar: "--color-admin-warn" },
  { key: "throughput", value: "38/phút", colorVar: "--color-success" },
  { key: "failed24h", value: "3", colorVar: "--color-admin-negative" },
];

// dc.html:452-457.
const WORKERS: Worker[] = [
  { id: "go-judge-w1", status: "running", load: 78, jobs: 3, meta: "Uptime 12 ngày · vCPU 4" },
  { id: "go-judge-w2", status: "running", load: 64, jobs: 2, meta: "Uptime 12 ngày · vCPU 4" },
  { id: "go-judge-w3", status: "overloaded", load: 93, jobs: 1, meta: "Uptime 6 giờ · vCPU 2" },
  { id: "go-judge-w4", status: "idle", load: 12, jobs: 0, meta: "Uptime 3 ngày · vCPU 4" },
];

// dc.html:471-475 (`state.toggles`).
const CONTROLS: ClusterControls = {
  paused: false,
  autoscale: true,
  highPriorityFirst: true,
};

// dc.html:477 minus the rejudge queue.
const LATENCY: QueueLatency[] = [
  { priority: "normal", label: "1,8s", percent: 30 },
  { priority: "high", label: "0,9s", percent: 16 },
];

// dc.html:506-512. #J-77404 and #J-77401 were "Chấm lại" — now normal.
const JOBS: QueueJob[] = [
  { id: "#J-77410", submissionId: "#SB-90412", language: "Python 3", priority: "high", status: "running", waitSeconds: 0.4, waitLabel: "0,4s" },
  { id: "#J-77409", submissionId: "#SB-90411", language: "C++ 17", priority: "high", status: "running", waitSeconds: 0.9, waitLabel: "0,9s" },
  { id: "#J-77406", submissionId: "#SB-90408", language: "Java 21", priority: "normal", status: "waiting", waitSeconds: 12, waitLabel: "12s" },
  { id: "#J-77404", submissionId: "#SB-90406", language: "Python 3", priority: "normal", status: "waiting", waitSeconds: 31, waitLabel: "31s" },
  { id: "#J-77401", submissionId: "#SB-90403", language: "C++ 17", priority: "normal", status: "waiting", waitSeconds: 46, waitLabel: "46s" },
  { id: "#J-77398", submissionId: "#SB-90399", language: "Java 21", priority: "normal", status: "sandboxError", waitSeconds: -1, waitLabel: "—" },
];

// dc.html:526-533.
const INFRA_EVENTS: InfraEvent[] = [
  { id: "inf-1", time: "15:41:02", level: "error", message: "go-judge worker 3 mất kết nối, 4 job được xếp lại hàng đợi", service: "go-judge" },
  { id: "inf-2", time: "15:38:44", level: "warn", message: "Lượt nộp #48207 vượt thời gian chạy trên testcase 14", service: "go-judge" },
  { id: "inf-3", time: "15:34:12", level: "error", message: "Gọi Claude API thất bại sau 3 lần thử, mã 529", service: "ai-gateway" },
  { id: "inf-4", time: "15:32:20", level: "warn", message: "Bộ nhớ worker 2 đạt 87% giới hạn 512 MB", service: "go-judge" },
  { id: "inf-5", time: "15:30:41", level: "info", message: "Sao lưu cơ sở dữ liệu hằng ngày hoàn tất, 4,2 GB", service: "db" },
  { id: "inf-6", time: "15:22:15", level: "info", message: "Worker go-judge-w4 khởi động lại sau khi mở rộng tự động", service: "go-judge" },
];

export function fetchQueuePage(): QueuePage {
  return {
    kpis: KPIS.map((kpi) => ({ ...kpi })),
    workers: WORKERS.map((worker) => ({ ...worker })),
    controls: { ...CONTROLS },
    latency: LATENCY.map((item) => ({ ...item })),
    jobs: JOBS.map((job) => ({ ...job })),
    infraEvents: INFRA_EVENTS.map((event) => ({ ...event })),
    workerCount: 4,
    maxWorkers: 8,
  };
}
