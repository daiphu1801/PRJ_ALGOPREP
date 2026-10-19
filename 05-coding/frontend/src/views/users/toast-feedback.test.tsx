// One toast assertion per migrated student view (DEC-2026-1003-toast-feedback-channel). Tests mock
// useT to return the key, so the assertions read `[tone, key]` pairs.
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { resetRecallAndBookmarkState } from "@/entities/interview-question";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { InterviewBankListView } from "./interview-bank-list/ui/interview-bank-list-view";
import { InterviewQuestionDetailView } from "./interview-question-detail/ui/interview-question-detail-view";
import { MockInterviewView } from "./mock-interview/ui/mock-interview-view";
import { MySubmissionsView } from "./my-submissions/ui/my-submissions-view";
import { ProblemDetailView } from "./problem-detail/ui/problem-detail-view";
import { ProblemListView } from "./problem-list/ui/problem-list-view";
import { SavedProblemsView } from "./saved-problems/ui/saved-problems-view";
import { DataExportCard } from "./settings/ui/data-export-card";
import { SolutionReviewView } from "./solution-review/ui/solution-review-view";
import { SubmissionResultView } from "./submission-result/ui/submission-result-view";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string) => key,
  useLocale: () => "vi",
}));
vi.mock("next/navigation", () => ({
  useParams: () => ({ problemId: "1", submissionId: "SB-90412" }),
  useRouter: () => ({ push: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
}));

function observe() {
  const toasts = renderHook(() => useToasts());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

afterEach(() => {
  vi.useRealTimers();
  act(() => toast.clear());
  resetRecallAndBookmarkState();
});

describe("student views raise toasts for operation results", () => {
  it("saved problems: un-saving offers Undo (no toast) and Enter in search", () => {
    const read = observe();
    render(<SavedProblemsView />);
    fireEvent.click(screen.getAllByText("table.btnUnsave")[0]!);
    // Un-save is confirmed by an info toast that carries the Undo action.
    expect(read()).toEqual([["info", "undo.message"]]);
    fireEvent.keyDown(screen.getByPlaceholderText("filter.searchPlaceholder"), {
      key: "Enter",
    });
    expect(read().at(-1)?.[0]).toBe("info");
  });

  it("problem list: Enter in search announces the result count", () => {
    const read = observe();
    render(<ProblemListView />);
    fireEvent.keyDown(screen.getByPlaceholderText("filter.searchPlaceholder"), {
      key: "Enter",
    });
    expect(read()[0]?.[0]).toBe("info");
  });

  it("my submissions: typing is silent, Enter announces", () => {
    const read = observe();
    render(<MySubmissionsView />);
    const search = screen.getByPlaceholderText("filter.searchPlaceholder");
    fireEvent.change(search, { target: { value: "two" } });
    expect(read()).toEqual([]);
    fireEvent.keyDown(search, { key: "Enter" });
    expect(read()[0]?.[0]).toBe("info");
  });

  it("interview bank: bookmark raises success", () => {
    const read = observe();
    render(<InterviewBankListView />);
    fireEvent.click(screen.getAllByLabelText("bookmarkLabel")[0]!);
    expect(read()[0]?.[0]).toBe("success");
  });

  it("interview question detail: bookmark raises success", () => {
    const read = observe();
    render(<InterviewQuestionDetailView questionId="IQ-014" />);
    fireEvent.click(screen.getByText("header.bookmark"));
    expect(read()).toEqual([["success", "toast.bookmarked"]]);
  });

  it("problem detail: save toggle and sample run", () => {
    vi.useFakeTimers();
    const read = observe();
    render(<ProblemDetailView />);
    // Problem 1 starts saved: first click un-saves, second saves again.
    fireEvent.click(screen.getByText("panel.btnUnsave"));
    expect(read()).toEqual([["success", "toast.unsaved"]]);
    fireEvent.click(screen.getByText("panel.btnSave"));
    expect(read().at(-1)).toEqual(["success", "toast.saved"]);
    fireEvent.click(screen.getByText("taskbar.btnRun"));
    act(() => void vi.advanceTimersByTime(500));
    // The mock run alternates pass and fail, so either tone is a valid result.
    expect(["success", "warning"]).toContain(read().at(-1)?.[0]);
  });

  it("submission result: copy without clipboard support raises error", () => {
    const read = observe();
    render(<SubmissionResultView />);
    fireEvent.click(screen.getByText("code.copy"));
    expect(read()).toEqual([["error", "code.copyFailed"]]);
  });

  it("mock interview: finishing early raises success", async () => {
    const read = observe();
    render(<MockInterviewView />);
    fireEvent.click(screen.getByText("1. Two Sum"));
    fireEvent.click(screen.getByText("startCta"));
    fireEvent.click(await screen.findByText("stopEarlyCta"));
    fireEvent.click(screen.getByText("earlyExitConfirm"));
    expect(read()).toEqual([["success", "toast.finished"]]);
  });

  it("solution review: applying the suggestion raises success", async () => {
    const read = observe();
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <SolutionReviewView />
      </QueryClientProvider>,
    );
    fireEvent.click(await screen.findByText("applyCta"));
    fireEvent.click(screen.getByText("applyConfirmConfirm"));
    expect(read()).toEqual([["success", "toast.applied"]]);
  });

  it("settings export: a failed export raises error", async () => {
    const read = observe();
    render(<DataExportCard />);
    // Force the download step to throw so the hook reports failure.
    URL.createObjectURL = () => {
      throw new Error("download blocked");
    };
    fireEvent.click(screen.getByText("dataExport.exportSubmissions"));
    await waitFor(() =>
      expect(read()).toEqual([["error", "dataExport.exportFailed"]]),
    );
  });
});
