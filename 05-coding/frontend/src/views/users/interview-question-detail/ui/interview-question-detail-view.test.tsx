// Smoke test (PROTOTYPE lane) — route renders for a known question, mode switch and the practice
// submit path are reachable. Full behavioural coverage per 04-tdd/interview_question_detail.md's
// AC-nn comes later, once that file exists.
import { describe, expect, it, vi } from "vitest";
import { act, fireEvent, render, renderHook, screen } from "@testing-library/react";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { InterviewQuestionDetailView } from "./interview-question-detail-view";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, params?: Record<string, unknown>) =>
    params ? `${key} ${JSON.stringify(params)}` : key,
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

describe("InterviewQuestionDetailView", () => {
  it("renders Study mode content for a known question", () => {
    render(<InterviewQuestionDetailView questionId="IQ-014" />);

    expect(
      screen.getByText(/Hash table xử lý collision bằng cách nào\?/),
    ).toBeInTheDocument();
    expect(screen.getByText("study.outlineLabel")).toBeInTheDocument();
  });

  it("shows the not-found state for an unknown questionId, instead of crashing", () => {
    render(<InterviewQuestionDetailView questionId="IQ-does-not-exist" />);

    expect(screen.getByText("notFound")).toBeInTheDocument();
  });

  it("switches to Practice mode and can submit a non-empty answer", () => {
    render(<InterviewQuestionDetailView questionId="IQ-014" />);

    fireEvent.click(screen.getByText("mode.practice"));
    fireEvent.change(screen.getByLabelText("practice.answerLabel"), {
      target: { value: "Chaining dùng linked list, open addressing dùng probing." },
    });
    fireEvent.click(screen.getByText("practice.btnSubmit"));

    expect(screen.getByText("practice.pendingState")).toBeInTheDocument();
  });

  it("locks Practice mode when the question has no rubric", () => {
    // IQ-040 is seeded with hasRubric: false.
    render(<InterviewQuestionDetailView questionId="IQ-040" />);

    const toasts = renderHook(() => useToasts());

    const practice = screen.getByText("mode.practice");
    expect(practice).toHaveAttribute("aria-disabled", "true");
    // Stays clickable: the click says why instead of doing nothing.
    fireEvent.click(practice);
    expect(toasts.result.current.map((item) => [item.tone, item.message])).toEqual([
      ["warning", "mode.practiceLockedReason"],
    ]);
    act(() => toast.clear());
  });
});
