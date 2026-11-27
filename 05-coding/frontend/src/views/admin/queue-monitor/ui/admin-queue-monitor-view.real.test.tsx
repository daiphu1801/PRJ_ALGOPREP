import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import { toast } from "@/shared/lib/toast-store";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { AdminQueueMonitorView } from "./admin-queue-monitor-view";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: () => unknown, real: () => Promise<T>) => real(),
}));
vi.mock("@/shared/api/stomp", () => ({ subscribeJudge: () => () => {} }));

const snapshot = {
  status: "NORMAL",
  generatedAt: new Date().toISOString(),
  pendingCount: 0,
  queueConsumptionPaused: false,
  autoscaleEnabled: true,
  avgWaitMillis: null,
  workerAutoscaleMax: 8,
  finishedLastMinute: 0,
  estimatedClearSeconds: null,
  consumers: null,
};
const jobs = {
  items: [],
  page: 1,
  pageSize: 20,
  totalCount: 0,
  counts: { waiting: 0, failed: 0, all: 0 },
};
const settings = {
  autoscaleJobThreshold: 20,
  jobsPerPage: 20,
  errorRetentionHours: 24,
  latencyCapSeconds: 10,
};

beforeEach(() => {
  resetApiMock();
  vi.restoreAllMocks();
});

it("keeps jobs and infrastructure available when the snapshot fails", async () => {
  apiMock.get.mockImplementation(async (path: string) => {
    if (path.endsWith("/jobs")) return jobs;
    if (path.endsWith("/settings")) return settings;
    if (path.endsWith("/infra-summary")) return { warnCount: 1, errorCount: 2 };
    throw new Error("snapshot unavailable");
  });
  render(withTestProviders(<AdminQueueMonitorView />));
  expect(
    await screen.findByText("Không tải được trạng thái hàng đợi", {
      selector: '[role="status"] span',
    }),
  ).toBeVisible();
  expect(await screen.findByText("Không có bài nộp đang chờ.")).toBeVisible();
  expect(
    await screen.findByRole("link", { name: "Xem lỗi hạ tầng" }),
  ).toHaveAttribute("href", "/admin/infra-events?level=error");
});

it("reports a partial refresh while keeping valid zeroes and distinguishing a missing worker report", async () => {
  let infraFails = false;
  apiMock.get.mockImplementation(async (path: string) => {
    if (path.endsWith("/snapshot")) return snapshot;
    if (path.endsWith("/jobs")) return jobs;
    if (path.endsWith("/settings")) return settings;
    if (!infraFails && path.endsWith("/infra-summary")) {
      return { warnCount: 0, errorCount: 0 };
    }
    throw new Error("infrastructure unavailable");
  });
  const success = vi.spyOn(toast, "success");
  const warning = vi.spyOn(toast, "warning");
  render(withTestProviders(<AdminQueueMonitorView />));
  expect(
    await screen.findByText("Chưa nhận được báo cáo từ worker chấm bài"),
  ).toBeVisible();
  expect(screen.getByText("0 lượt/phút")).toBeVisible();
  expect(
    await screen.findByText("0 cảnh báo, 0 lỗi trong 24 giờ qua"),
  ).toBeVisible();
  infraFails = true;
  fireEvent.click(screen.getByRole("button", { name: "Làm mới" }));
  expect(
    await screen.findByText("Không tải được số liệu hạ tầng"),
  ).toBeVisible();
  expect(
    screen.getByRole("link", { name: "Xem tất cả sự kiện" }),
  ).toBeVisible();
  expect(screen.getByText("Bình thường")).toBeVisible();
  await waitFor(() =>
    expect(warning).toHaveBeenCalledWith("Một phần dữ liệu chưa được làm mới"),
  );
  expect(success).not.toHaveBeenCalled();
});

it("offers a retry for the snapshot and shows its figures once it answers (DD ADM0401 section 3)", async () => {
  let snapshotFails = true;
  apiMock.get.mockImplementation(async (path: string) => {
    if (path.endsWith("/jobs")) return jobs;
    if (path.endsWith("/settings")) return settings;
    if (path.endsWith("/infra-summary")) return { warnCount: 0, errorCount: 0 };
    if (snapshotFails) throw new Error("snapshot unavailable");
    return snapshot;
  });
  render(withTestProviders(<AdminQueueMonitorView />));
  const retry = await screen.findByRole("button", { name: "Thử lại" });
  expect(screen.queryByText("0 lượt/phút")).toBeNull();

  snapshotFails = false;
  fireEvent.click(retry);

  expect(await screen.findByText("0 lượt/phút")).toBeVisible();
  expect(screen.queryByRole("button", { name: "Thử lại" })).toBeNull();
});

it("does not keep showing old figures after a later snapshot read fails", async () => {
  let snapshotFails = false;
  apiMock.get.mockImplementation(async (path: string) => {
    if (path.endsWith("/jobs")) return jobs;
    if (path.endsWith("/settings")) return settings;
    if (path.endsWith("/infra-summary")) return { warnCount: 0, errorCount: 0 };
    if (snapshotFails) throw new Error("snapshot unavailable");
    return snapshot;
  });
  render(withTestProviders(<AdminQueueMonitorView />));
  expect(await screen.findByText("0 lượt/phút")).toBeVisible();

  snapshotFails = true;
  fireEvent.click(screen.getByRole("button", { name: "Làm mới" }));

  expect(await screen.findByRole("button", { name: "Thử lại" })).toBeVisible();
  expect(screen.queryByText("0 lượt/phút")).toBeNull();
});
