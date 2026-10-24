// Smoke test (PROTOTYPE lane) — route renders for a known question, mode switch and the practice
// submit path are reachable. Full behavioural coverage per 04-tdd/interview_question_detail.md's
// AC-nn comes later, once that file exists.
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactElement } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
} from "@testing-library/react";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { resetRecallAndBookmarkState } from "@/entities/interview-question";
import { InterviewQuestionDetailView } from "./interview-question-detail-view";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, params?: Record<string, unknown>) =>
    params ? `${key} ${JSON.stringify(params)}` : key,
  useLocale: () => "vi",
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

// The bank loads through TanStack Query, so each test renders inside a fresh client.
function renderWithQuery(ui: ReactElement) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>{ui}</QueryClientProvider>,
  );
}

afterEach(() => resetRecallAndBookmarkState());

describe("InterviewQuestionDetailView", () => {
  it("renders Study mode content for a known question", async () => {
    renderWithQuery(<InterviewQuestionDetailView questionId="IQ-014" />);
    await screen.findByText("header.linkBack");

    expect(
      screen.getByText(/Hash table xử lý collision bằng cách nào\?/),
    ).toBeInTheDocument();
    expect(screen.getByText("study.outlineLabel")).toBeInTheDocument();
  });

  it("shows the not-found state for an unknown questionId, instead of crashing", async () => {
    renderWithQuery(<InterviewQuestionDetailView questionId="IQ-does-not-exist" />);
    await screen.findByText("header.linkBack");

    expect(screen.getByText("notFound")).toBeInTheDocument();
  });

  it("switches to Practice mode and can submit a non-empty answer", async () => {
    renderWithQuery(<InterviewQuestionDetailView questionId="IQ-014" />);
    await screen.findByText("header.linkBack");

    fireEvent.click(screen.getByText("mode.practice"));
    fireEvent.change(screen.getByLabelText("practice.answerLabel"), {
      target: {
        value: "Chaining dùng linked list, open addressing dùng probing.",
      },
    });
    fireEvent.click(screen.getByText("practice.btnSubmit"));

    expect(screen.getByText("practice.pendingState")).toBeInTheDocument();
  });

  it("locks Practice mode when the question has no rubric", async () => {
    // IQ-040 is seeded with hasRubric: false.
    renderWithQuery(<InterviewQuestionDetailView questionId="IQ-040" />);
    await screen.findByText("header.linkBack");

    const toasts = renderHook(() => useToasts());

    const practice = screen.getByText("mode.practice");
    // Stays clickable: the click says why instead of doing nothing.
    fireEvent.click(practice);
    expect(
      toasts.result.current.map((item) => [item.tone, item.message]),
    ).toEqual([["warning", "mode.practiceLockedReason"]]);
    act(() => toast.clear());
  });

  it("does not fire the simulated feedback after the screen is left", async () => {
    const toasts = renderHook(() => useToasts());
    const view = renderWithQuery(
      <InterviewQuestionDetailView questionId="IQ-014" />,
    );
    await screen.findByText("header.linkBack");
    // Fake timers start only after the question has loaded, so the query is not frozen.
    vi.useFakeTimers();
    try {
      fireEvent.click(screen.getByText("mode.practice"));
      fireEvent.change(screen.getByLabelText("practice.answerLabel"), {
        target: { value: "Một câu trả lời." },
      });
      fireEvent.click(screen.getByText("practice.btnSubmit"));
      view.unmount();
      act(() => {
        vi.advanceTimersByTime(2000);
      });
      expect(toasts.result.current).toEqual([]);
    } finally {
      vi.useRealTimers();
      act(() => toast.clear());
    }
  });

  it("keeps a rating made on the detail screen after the screen is re-opened", async () => {
    const first = renderWithQuery(
      <InterviewQuestionDetailView questionId="IQ-014" />,
    );
    await screen.findByText("header.linkBack");
    fireEvent.click(screen.getByText("study.recall.forgotten"));
    first.unmount();
    renderWithQuery(<InterviewQuestionDetailView questionId="IQ-014" />);
    await screen.findByText("header.linkBack");
    expect(await screen.findByText(/study.nextReviewHint/)).toBeInTheDocument();
    act(() => toast.clear());
  });

  it("toggles the bookmark label, shows History in both modes and Keywords only in study", async () => {
    renderWithQuery(<InterviewQuestionDetailView questionId="IQ-014" />);
    await screen.findByText("header.linkBack");

    const bookmark = screen.getByRole("button", { name: "header.bookmark" });
    expect(bookmark).toHaveAttribute("aria-pressed", "false");
    fireEvent.click(bookmark);
    expect(
      screen.getByRole("button", { name: "header.bookmarked" }),
    ).toHaveAttribute("aria-pressed", "true");

    expect(screen.getByText("attempts.title")).toBeInTheDocument();
    expect(screen.getByText("study.keywordsLabel")).toBeInTheDocument();
    fireEvent.click(screen.getByText("mode.practice"));
    expect(screen.getByText("attempts.title")).toBeInTheDocument();
    expect(screen.queryByText("study.keywordsLabel")).not.toBeInTheDocument();
  });
});
