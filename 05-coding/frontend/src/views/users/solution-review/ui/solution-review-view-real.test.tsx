// USR0301 against the REAL api path (F5 #4 plus F4 detail for the source): only the HTTP layer is
// faked, so the real modules, the AIR-* handling and the real vi texts are what is asserted.
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../messages/vi.json";
import { ApiError } from "@/shared/api";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: () => unknown, real: () => Promise<T>) => real(),
}));

const SUBMISSION = "6f1c2b0e-0000-4000-8000-000000000001";
const push = vi.fn();
vi.mock("next/navigation", () => ({
  useSearchParams: () => new URLSearchParams(),
  useParams: () => ({ submissionId: SUBMISSION }),
  useRouter: () => ({ push }),
}));

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { SolutionReviewView } from "./solution-review-view";

const m = viMessages.solutionReview;
const errors = viMessages.apiErrors;
const REVIEW_PATH = `/ai/submissions/${SUBMISSION}/solution-review`;
const DETAIL_PATH = `/judge/submissions/${SUBMISSION}`;

const review = (over: Record<string, unknown> = {}) => ({
  id: "r1",
  submissionId: SUBMISSION,
  problemId: "p1",
  language: "PYTHON",
  createdAt: "2026-11-12T03:00:00Z",
  servedFromCache: false,
  isReferenceOnly: true,
  readabilityScore: 4,
  schemaVersion: "1",
  approachTitle: "hash map",
  verdict: "IMPROVABLE",
  isApproachOptimal: false,
  complexity: {
    actualTime: "O(n^2)",
    optimalTime: "O(n)",
    timeExplanation: "Hai vòng lặp lồng nhau",
    actualSpace: "O(1)",
    optimalSpace: null,
    spaceExplanation: "Không cấp thêm bộ nhớ",
  },
  strengths: ["Dễ đọc"],
  improvements: ["Dùng dict để tra cứu"],
  edgeCases: ["Mảng rỗng"],
  followUpTopics: ["hash map"],
  codeDiff: {
    suggestedCode: "def f(a):\n    return seen.get(a)\n",
    explanation: "Gộp bước tra cứu",
  },
  ...over,
});

const judgeDetail = (over: Record<string, unknown> = {}) => ({
  submissionId: SUBMISSION,
  status: "ACCEPTED",
  language: "PYTHON",
  submissionModel: "FUNCTION_WRAPPER",
  submittedAt: "2026-11-12T02:00:00Z",
  sourceCode: "def f(a):   \n    return a\n",
  compileErrorMessage: null,
  passedCount: 3,
  totalCount: 3,
  runtimeMs: 10,
  memoryKb: 100,
  beatsPercent: 50,
  beatsSampleSize: 10,
  problem: {
    problemId: "p1",
    code: "P-1",
    title: "Two Sum thật",
    slug: "two-sum",
    levelDisplayName: "Dễ",
    withdrawn: false,
  },
  testcaseDetail: { available: true, unavailableReason: null, items: [] },
  ...over,
});

function serve(opts: {
  review?: () => unknown | Error;
  detail?: () => unknown | Error;
} = {}) {
  apiMock.post.mockImplementation(async (path: string) => {
    if (path !== REVIEW_PATH) throw new Error(`unexpected POST ${path}`);
    const value = (opts.review ?? (() => review()))();
    if (value instanceof Error) throw value;
    return value;
  });
  apiMock.get.mockImplementation(async (path: string) => {
    if (path === "/identity/me/profile") return {};
    if (path !== DETAIL_PATH) throw new Error(`unexpected GET ${path}`);
    const value = (opts.detail ?? (() => judgeDetail()))();
    if (value instanceof Error) throw value;
    return value;
  });
}

const toastsOf = () => renderHook(() => useToasts()).result;
const lastToast = (toasts: { current: readonly { message: string }[] }) =>
  toasts.current.at(-1)?.message;
const reviewCalls = () =>
  apiMock.post.mock.calls.filter(([path]) => path === REVIEW_PATH);

beforeEach(() => {
  resetApiMock();
  serve();
  push.mockClear();
  window.localStorage.clear();
  act(() => toast.clear());
});
afterEach(() => {
  vi.useRealTimers();
});

describe("SolutionReviewView with the real API", () => {
  it("shows the loading note first, then the report joined with the F4 title and code", async () => {
    render(withTestProviders(<SolutionReviewView />));

    expect(screen.getByText(m.loadingLabel)).toBeInTheDocument();
    expect(await screen.findByText("Two Sum thật")).toBeInTheDocument();
    expect(screen.getAllByText("hash map").length).toBeGreaterThan(0);
    expect(screen.getByText("O(n^2)")).toBeInTheDocument();
    expect(screen.getByText(m.eduNoticeTitle)).toBeInTheDocument();
    // The source block and the diff come from the F4 answer; the suggestion is one added line.
    expect(screen.getByText(m.sourceCodeTitle)).toBeInTheDocument();
    expect(screen.getByRole("group", { name: m.diffTitle })).toHaveTextContent(
      "return seen.get(a)",
    );
    expect(reviewCalls()).toHaveLength(1);
    expect(reviewCalls()[0]![1]).toBeUndefined();
  });

  it("does not call the generate endpoint a second time on its own", async () => {
    render(withTestProviders(<SolutionReviewView />));
    await screen.findByText("Two Sum thật");

    act(() => {
      window.dispatchEvent(new Event("focus"));
      window.dispatchEvent(new Event("online"));
    });
    await new Promise((resolve) => setTimeout(resolve, 20));

    expect(reviewCalls()).toHaveLength(1);
  });

  it("applies the suggestion into the draft of the real (problem, language, model)", async () => {
    serve({ detail: () => judgeDetail({ submissionModel: "STANDARD_IO" }) });
    render(withTestProviders(<SolutionReviewView />));
    await screen.findByText("Two Sum thật");

    fireEvent.click(screen.getByRole("button", { name: m.applyCta }));
    fireEvent.click(screen.getByRole("button", { name: m.applyConfirmConfirm }));

    expect(window.localStorage.getItem("algoprep-draft:p1:python:stdio")).toContain(
      "seen.get(a)",
    );
    expect(push).toHaveBeenCalledWith("/workspace/p1");
  });

  it("hides the diff and disables Apply when the F4 call fails, but keeps the report", async () => {
    serve({ detail: () => new ApiError("JDG-101", 404) });
    render(withTestProviders(<SolutionReviewView />));

    expect(await screen.findByText("O(n^2)")).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getAllByText(m.sourceUnavailable).length).toBeGreaterThan(0),
    );
    expect(screen.queryByRole("group", { name: m.diffTitle })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: m.applyCta })).toBeDisabled();
  });

  it("hides the source block when F4 answers without a problem", async () => {
    serve({ detail: () => judgeDetail({ problem: null }) });
    render(withTestProviders(<SolutionReviewView />));

    expect(await screen.findByText("O(n^2)")).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getAllByText(m.sourceUnavailable).length).toBeGreaterThan(0),
    );
    expect(screen.getByRole("button", { name: m.applyCta })).toBeDisabled();
  });

  it("labels a withdrawn problem next to the title and keeps Apply", async () => {
    serve({
      detail: () =>
        judgeDetail({
          problem: { ...judgeDetail().problem, withdrawn: true },
        }),
    });
    render(withTestProviders(<SolutionReviewView />));

    expect(await screen.findByText(m.withdrawn)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: m.applyCta })).toBeEnabled();
  });

  it("hides the diff card and Apply when the approach is already optimal (codeDiff null)", async () => {
    serve({
      review: () => review({ verdict: "OPTIMAL", isApproachOptimal: true, codeDiff: null }),
    });
    render(withTestProviders(<SolutionReviewView />));

    await screen.findByText("Two Sum thật");
    expect(screen.queryByRole("button", { name: m.applyCta })).not.toBeInTheDocument();
    expect(screen.queryByText(m.diffTitle)).not.toBeInTheDocument();
  });

  describe("errors", () => {
    it.each([
      ["AIR-101", 404],
      ["AIR-102", 422],
      ["AIR-302", 403],
    ])("%s opens one merged notice with no Retry and a toast", async (code, status) => {
      serve({ review: () => new ApiError(code, status) });
      const toasts = toastsOf();
      render(withTestProviders(<SolutionReviewView />));

      expect(await screen.findByText(m.errorUnavailableTitle)).toBeInTheDocument();
      expect(screen.queryByRole("button", { name: m.retry })).not.toBeInTheDocument();
      expect(screen.getByRole("link", { name: m.backToSubmissions })).toHaveAttribute(
        "href",
        "/submissions",
      );
      expect(lastToast(toasts)).toBe(m.toast.unavailable);
      expect(reviewCalls()).toHaveLength(1);
    });

    it("AIR-401 shows the budget notice, no Retry, a link out and a toast", async () => {
      serve({ review: () => new ApiError("AIR-401", 429) });
      const toasts = toastsOf();
      render(withTestProviders(<SolutionReviewView />));

      expect(await screen.findByText(m.errorBudgetLockedTitle)).toBeInTheDocument();
      expect(screen.queryByRole("button", { name: m.retry })).not.toBeInTheDocument();
      expect(lastToast(toasts)).toBe(m.toast.budgetLocked);
    });

    it("AIR-402 waits for retryAfterSeconds: Retry is disabled with a countdown, then enabled", async () => {
      serve({
        review: () => new ApiError("AIR-402", 429, "x", { retryAfterSeconds: 2 }),
      });
      const toasts = toastsOf();
      render(withTestProviders(<SolutionReviewView />));

      expect(await screen.findByText(m.errorRateLimitedTitle)).toBeInTheDocument();
      expect(screen.getByText(/Hãy thử lại sau 2 giây/)).toBeInTheDocument();
      const wait = screen.getByRole("button", { name: "Thử lại sau 2 giây" });
      expect(wait).toBeDisabled();
      expect(lastToast(toasts)).toBe("Bạn yêu cầu hơi nhanh. Thử lại sau 2 giây.");
      // The AIR-402 text of the shared map exists too, in case another screen shows it.
      expect(errors["AIR-402"]).toBeTruthy();

      expect(await screen.findByRole("button", { name: m.retry }, { timeout: 4000 })).toBeEnabled();
      expect(reviewCalls()).toHaveLength(1);
    });

    it("AIR-404 without details falls back to a 20 second wait", async () => {
      serve({ review: () => new ApiError("AIR-404", 429) });
      render(withTestProviders(<SolutionReviewView />));

      expect(await screen.findByRole("button", { name: "Thử lại sau 20 giây" })).toBeDisabled();
      expect(errors["AIR-404"]).toBeTruthy();
    });

    it.each([
      ["AIR-501", 503],
      ["AIR-502", 502],
    ])("%s is transient: warning, a toast, and one Retry that calls once more", async (code, status) => {
      let attempt = 0;
      serve({
        review: () => (attempt++ === 0 ? new ApiError(code, status) : review()),
      });
      const toasts = toastsOf();
      render(withTestProviders(<SolutionReviewView />));

      expect(await screen.findByText(m.errorTransientTitle)).toBeInTheDocument();
      expect(lastToast(toasts)).toBe(m.toast.transientError);
      fireEvent.click(screen.getByRole("button", { name: m.retry }));

      expect(await screen.findByText("Two Sum thật")).toBeInTheDocument();
      expect(reviewCalls()).toHaveLength(2);
      expect(errors[code as "AIR-501" | "AIR-502"]).toBeTruthy();
    });

    it("a failure with an unknown code shows the generic text from apiErrors, never the server text", async () => {
      serve({ review: () => new ApiError("UNKNOWN_ERROR", 0, "ECONNRESET stack trace") });
      const toasts = toastsOf();
      render(withTestProviders(<SolutionReviewView />));

      expect(await screen.findByText(m.errorTransientTitle)).toBeInTheDocument();
      expect(lastToast(toasts)).toBe(errors.UNKNOWN_ERROR);
      expect(screen.queryByText(/ECONNRESET/)).not.toBeInTheDocument();
    });

    it("AIR-103 keeps the skeleton, shows no error, and the second call returns the report", async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true });
      let attempt = 0;
      serve({
        review: () => (attempt++ === 0 ? new ApiError("AIR-103", 409) : review()),
      });
      const toasts = toastsOf();
      render(withTestProviders(<SolutionReviewView />));

      await waitFor(() => expect(reviewCalls()).toHaveLength(1));
      expect(screen.getByText(m.loadingLabel)).toBeInTheDocument();
      expect(screen.queryByText(m.errorTransientTitle)).not.toBeInTheDocument();
      expect(toasts.current).toHaveLength(0);

      await act(async () => {
        await vi.advanceTimersByTimeAsync(3000);
      });

      expect(await screen.findByText("Two Sum thật")).toBeInTheDocument();
      expect(reviewCalls()).toHaveLength(2);
      expect(toasts.current).toHaveLength(0);
      expect(errors["AIR-103"]).toBeTruthy();
    });

    it("cannot be submitted twice while the long call runs (no Retry button during loading)", async () => {
      serve({ review: () => new Promise(() => undefined) as never });
      render(withTestProviders(<SolutionReviewView />));

      expect(screen.getByText(m.loadingLabel)).toBeInTheDocument();
      expect(screen.queryByRole("button", { name: m.retry })).not.toBeInTheDocument();
      expect(reviewCalls()).toHaveLength(1);
    });
  });
});
