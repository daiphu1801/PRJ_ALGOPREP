// ADM0302 against the REAL api path (F5 #18, #27 to #31): only the HTTP layer is faked, so the real
// modules, the error mapping and the real vi texts are what the assertions read. Mock-mode behaviour stays
// in views/admin/ai-usage-budget.test.tsx and admin-toasts.test.tsx.
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../messages/vi.json";
import { ApiError } from "@/shared/api";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);
// `useMockQuery` reads the flag from `mock-mode` directly, not through the barrel mocked above.
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: () => unknown, real: () => Promise<T>) => real(),
}));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { AdminAiUsageView } from "./admin-ai-usage-view";

const m = viMessages.adminAiUsage;
const apiErrors = viMessages.apiErrors as Record<string, string>;
const U = "/ai/admin/usage";
const BUDGET = "/ai/admin/token-budget";
const ALERT = "22222222-2222-4222-8222-222222222222";

const budgetDto = (over: Record<string, unknown> = {}) => ({
  limitTokens: 52_000_000,
  usedTokens: 38_400_000,
  periodStart: "2026-11-01",
  lockedAt: null,
  isLocked: false,
  projectedRunOutDate: "2026-11-28",
  featureBreakdown: [
    { featureCode: "SOLUTION_REVIEW", tokens: 20_900_000 },
    { featureCode: "MOCK_INTERVIEW", tokens: 11_800_000 },
    { featureCode: "GENERATE_TESTCASE", tokens: 3_700_000 },
    { featureCode: "PRACTICE_FEEDBACK", tokens: 2_000_000 },
  ],
  warnPercent: 70,
  ...over,
});

const anomaly = {
  alertType: "ANOMALY",
  alertId: ALERT,
  status: "OPEN",
  callCount: 214,
  ratio: 7,
  displayName: "nguyenvana",
};

let state: {
  budget: Record<string, unknown>;
  alerts: Record<string, unknown>[];
  fail: Record<string, ApiError>;
};

function routes() {
  apiMock.get.mockImplementation(async (path: string) => {
    if (state.fail[path]) throw state.fail[path];
    if (path === BUDGET) return state.budget;
    if (path === `${U}/daily`)
      return {
        rangeDays: 14,
        points: [
          {
            date: "2026-11-10",
            tokensByFeature: { SOLUTION_REVIEW: 100, MOCK_INTERVIEW: 50 },
          },
          { date: "2026-11-11", tokensByFeature: {} },
        ],
      };
    if (path === `${U}/alerts`) return { items: state.alerts };
    if (path === `${U}/top-users`)
      return {
        items: [
          {
            userId: "a",
            displayName: "Nguyễn Văn An",
            tokens: 1_840_000,
            calls: 214,
            costUsd: 8.7,
          },
          {
            userId: "b",
            displayName: null,
            tokens: 5000,
            calls: 3,
            costUsd: null,
          },
        ],
      };
    if (path === `${U}/top-problems`)
      return {
        items: [
          {
            problemId: "p1",
            problemTitle: "Two Sum",
            levelCode: "EASY",
            levelDisplayName: "Dễ",
            tokens: 2_100_000,
            calls: 412,
            avgTokensPerCall: 5100,
          },
          {
            problemId: "p2",
            problemTitle: null,
            levelCode: null,
            levelDisplayName: null,
            tokens: 900,
            calls: 2,
            avgTokensPerCall: 450,
          },
        ],
      };
    throw new Error(`unexpected GET ${path}`);
  });
}

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

const gets = (path: string, ...rest: unknown[]) =>
  apiMock.get.mock.calls.filter(
    (call) => call[0] === path && (rest.length === 0 || call[1] === rest[0]),
  ).length;

beforeEach(() => {
  resetApiMock();
  state = { budget: budgetDto(), alerts: [anomaly], fail: {} };
  routes();
});

describe("ADM0302 with the real API", () => {
  it("loads every block from its own endpoint, with the server default range of 14 days", async () => {
    render(withTestProviders(<AdminAiUsageView />));
    expect(await screen.findByText("Nguyễn Văn An")).toBeInTheDocument();
    expect(apiMock.get).toHaveBeenCalledWith(`${U}/daily`, { rangeDays: 14 });
    expect(apiMock.get).toHaveBeenCalledWith(BUDGET);
    expect(apiMock.get).toHaveBeenCalledWith(`${U}/alerts`);
    expect(apiMock.get).toHaveBeenCalledWith(`${U}/top-users`);
    expect(apiMock.get).toHaveBeenCalledWith(`${U}/top-problems`);
    // Budget panel: used and cap in millions, run-out date from the server.
    expect(await screen.findByText("Dự kiến hết 28/11")).toBeInTheDocument();
    expect(
      screen.getByRole("progressbar", { name: m.budgetTitle }),
    ).toHaveAttribute("aria-valuenow", "74");
    // Rankings: tokens and cost formatted, unknown name and unknown cost as a dash.
    expect(screen.getByText("1,84 tr")).toBeInTheDocument();
    expect(screen.getByText("8,7 USD")).toBeInTheDocument();
    expect(screen.getByText("Two Sum")).toBeInTheDocument();
    expect(screen.getByText("Dễ")).toBeInTheDocument();
    const users = screen.getByRole("table", { name: m.topUsersTitle });
    expect(within(users).getAllByText("-").length).toBeGreaterThanOrEqual(2);
  });

  it("asks only the daily endpoint again when the range changes; the monthly budget is not refetched", async () => {
    render(withTestProviders(<AdminAiUsageView />));
    await screen.findByText("Nguyễn Văn An");
    fireEvent.click(screen.getByRole("button", { name: m.range["30d"] }));
    await waitFor(() =>
      expect(apiMock.get).toHaveBeenCalledWith(`${U}/daily`, { rangeDays: 30 }),
    );
    expect(gets(BUDGET)).toBeLessThanOrEqual(2);
    expect(gets(`${U}/top-users`)).toBe(1);
    expect(gets(`${U}/top-problems`)).toBe(1);
  });

  it("shows the anomaly alert with the server's name, count and ratio, and the derived budget alert", async () => {
    state.alerts = [
      { alertType: "BUDGET", alertId: null, usedPercent: 74 },
      anomaly,
    ];
    render(withTestProviders(<AdminAiUsageView />));
    expect(
      await screen.findByText(m.alert.budgetRunningOut.title),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        "nguyenvana gọi 214 lượt trong 24 giờ, gấp 7 lần trung bình. Cần quản trị viên xem lại, hệ thống không tự khoá.",
      ),
    ).toBeInTheDocument();
    // Only the anomaly alert can be marked.
    expect(
      screen.getAllByRole("button", { name: m.markReviewed }),
    ).toHaveLength(1);
  });

  it("replaces the running-out alert with the lock when the budget is locked", async () => {
    state.budget = budgetDto({ isLocked: true, projectedRunOutDate: null });
    state.alerts = [{ alertType: "BUDGET", alertId: null }];
    render(withTestProviders(<AdminAiUsageView />));
    expect(
      await screen.findByText(m.alert.budgetLocked.title),
    ).toBeInTheDocument();
    expect(screen.queryByText(m.alert.budgetRunningOut.title)).toBeNull();
    expect(screen.getByText(m.budgetLocked)).toBeInTheDocument();
  });

  it("marks the alert reviewed with a bodyless POST, toasts, and refetches the list", async () => {
    apiMock.post.mockImplementation(async () => {
      state.alerts = [];
      return { alertId: ALERT, status: "REVIEWED", alreadyReviewed: false };
    });
    const read = watch();
    render(withTestProviders(<AdminAiUsageView />));
    fireEvent.click(
      await screen.findByRole("button", { name: m.markReviewed }),
    );
    await waitFor(() =>
      expect(read().at(-1)).toEqual(["success", m.markReviewedDone]),
    );
    expect(apiMock.post).toHaveBeenCalledWith(`${U}/alerts/${ALERT}/review`);
    expect(await screen.findByText(m.noAlerts)).toBeInTheDocument();
    expect(gets(`${U}/alerts`)).toBe(2);
  });

  it.each([
    ["AIR-120", 404],
    ["AIR-301", 403],
  ])(
    "shows the %s text when marking fails and never a success toast",
    async (code, status) => {
      apiMock.post.mockRejectedValue(new ApiError(code, status));
      const read = watch();
      render(withTestProviders(<AdminAiUsageView />));
      fireEvent.click(
        await screen.findByRole("button", { name: m.markReviewed }),
      );
      await waitFor(() => expect(read()).toEqual([["error", apiErrors[code]]]));
      // AIR-120 means the alert is gone: the list is refetched; AIR-301 keeps the alert unreviewed.
      if (code === "AIR-120")
        await waitFor(() => expect(gets(`${U}/alerts`)).toBe(2));
      else {
        expect(gets(`${U}/alerts`)).toBe(1);
        expect(
          screen.getByRole("button", { name: m.markReviewed }),
        ).toBeEnabled();
      }
    },
  );

  it.each([
    ["daily", `${U}/daily`, "AIR-V19"],
    ["alerts", `${U}/alerts`, "AIR-301"],
    ["top users", `${U}/top-users`, "AIR-V02"],
    ["top problems", `${U}/top-problems`, "AIR-V02"],
    ["budget", BUDGET, "AIR-301"],
  ])(
    "a failing %s block shows its %s... reason and a retry, the other blocks stay readable",
    async (_name, path, code) => {
      state.fail[path] = new ApiError(code, 400);
      render(withTestProviders(<AdminAiUsageView />));
      expect(
        (await screen.findAllByText(apiErrors[code]!)).length,
      ).toBeGreaterThan(0);
      // Blocks that did not fail still render their data.
      if (path !== `${U}/top-users`)
        expect(await screen.findByText("Nguyễn Văn An")).toBeInTheDocument();
      else expect(await screen.findByText("Two Sum")).toBeInTheDocument();
      // Retry refreshes only the failed endpoint.
      const before = gets(path);
      const retry = screen
        .getAllByRole("button", { name: viMessages.common.retry })
        .at(0)!;
      state.fail = {};
      fireEvent.click(retry);
      await waitFor(() => expect(gets(path)).toBe(before + 1));
    },
  );
});
