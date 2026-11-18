// View-level tests of USR0104 against the REAL api path (03-dd/api/F2_problem-bank.md #6, #11 plus the
// F4 limits and own-history reads), only the HTTP layer is faked. What matters: the page is the
// server's, an unknown id reads as not found, and the bookmark flips only after the server confirmed.
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
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
const push = vi.fn();
vi.mock("next/navigation", () => ({
  useParams: () => ({ problemId: "p1" }),
  useRouter: () => ({ push }),
}));

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { ProblemOverviewView } from "./problem-overview-view";

const d = viMessages.problemDetail;
const apiErrors = viMessages.apiErrors as Record<string, string>;

const OVERVIEW = {
  problemId: "p1",
  code: "P-1",
  title: "Two Sum",
  levelCode: "EASY",
  levelDisplayName: "Dễ",
  statementMd: "Cho mảng số nguyên.",
  constraintsMd: "",
  topicNames: ["Mảng"],
  acRate: null,
  hiddenTestcaseCount: 7,
  bookmarked: false,
  examples: [{ input: "1 2", output: "3", explanationMd: null }],
};
const LIMITS = {
  limits: ["JAVA", "CPP", "PYTHON"].map((language) => ({
    language,
    timeLimitMs: 1000,
    memoryLimitMb: 256,
  })),
};
const HISTORY = { items: [] };

function route(overview: unknown = OVERVIEW) {
  apiMock.get.mockImplementation(async (path: string) => {
    if (path.endsWith("/overview")) {
      if (overview instanceof Error) throw overview;
      return overview;
    }
    if (path.endsWith("/execution-limits")) return LIMITS;
    return HISTORY;
  });
}

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

describe("ProblemOverviewView with the real API", () => {
  beforeEach(() => {
    resetApiMock();
    push.mockReset();
    route();
  });

  it("renders the server's overview: hidden count only, acRate null as a dash, no invented limits", async () => {
    render(withTestProviders(<ProblemOverviewView />));

    expect(
      await screen.findByRole("heading", { level: 1, name: "Two Sum" }),
    ).toBeInTheDocument();
    expect(apiMock.get).toHaveBeenCalledWith(
      "/problem-bank/problems/p1/overview",
    );
    expect(apiMock.get).toHaveBeenCalledWith(
      "/judge/problems/p1/execution-limits",
    );
    expect(screen.getByText("7")).toBeInTheDocument();
    expect(screen.getByText("-")).toBeInTheDocument();
    expect(screen.getAllByText("1000 ms")).toHaveLength(3);
  });

  it("reads PRB-010 as not found", async () => {
    route(new ApiError("PRB-010", 404));
    render(withTestProviders(<ProblemOverviewView />));
    expect(await screen.findByText(d.empty.title)).toBeInTheDocument();
  });

  it("fails the page when the F4 limits read fails, never showing made-up limits", async () => {
    apiMock.get.mockImplementation(async (path: string) => {
      if (path.endsWith("/overview")) return OVERVIEW;
      throw new ApiError("UNKNOWN_ERROR", 503);
    });
    render(withTestProviders(<ProblemOverviewView />));
    await waitFor(() => expect(screen.queryByText("1000 ms")).toBeNull());
    expect(screen.queryByRole("heading", { level: 1 })).toBeNull();
  });

  it("bookmarks with the wanted state, flips after the server answers and toasts", async () => {
    apiMock.put.mockResolvedValue({ bookmarked: true });
    const toasts = watch();
    render(withTestProviders(<ProblemOverviewView />));
    fireEvent.click(
      await screen.findByRole("button", { name: d.panel.btnSave }),
    );

    await waitFor(() =>
      expect(apiMock.put).toHaveBeenCalledWith(
        "/problem-bank/me/bookmarks/p1/state",
        { bookmarked: true },
      ),
    );
    expect(
      await screen.findByRole("button", { name: d.panel.btnUnsave }),
    ).toHaveAttribute("aria-pressed", "true");
    expect(toasts().at(-1)).toEqual(["success", d.toast.saved]);
  });

  it("undo after removing sends bookmarked true again", async () => {
    route({ ...OVERVIEW, bookmarked: true });
    apiMock.put
      .mockResolvedValueOnce({ bookmarked: false })
      .mockResolvedValueOnce({ bookmarked: true });
    render(withTestProviders(<ProblemOverviewView />));
    fireEvent.click(
      await screen.findByRole("button", { name: d.panel.btnUnsave }),
    );
    await screen.findByRole("button", { name: d.panel.btnSave });
    expect(apiMock.put).toHaveBeenLastCalledWith(
      "/problem-bank/me/bookmarks/p1/state",
      { bookmarked: false },
    );
    const undo = renderHook(() => useToasts()).result.current.at(-1);
    act(() => undo?.action?.onClick());
    await waitFor(() =>
      expect(apiMock.put).toHaveBeenLastCalledWith(
        "/problem-bank/me/bookmarks/p1/state",
        { bookmarked: true },
      ),
    );
  });

  it("keeps the button state and toasts the PRB-010 text when the problem was withdrawn meanwhile", async () => {
    apiMock.put.mockRejectedValue(new ApiError("PRB-010", 404));
    const toasts = watch();
    render(withTestProviders(<ProblemOverviewView />));
    fireEvent.click(
      await screen.findByRole("button", { name: d.panel.btnSave }),
    );

    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", apiErrors["PRB-010"]]),
    );
    expect(
      screen.getByRole("button", { name: d.panel.btnSave }),
    ).toHaveAttribute("aria-pressed", "false");
  });
});
