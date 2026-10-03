// Toasts on the interview question list: delete result and explicit search.
import { beforeEach, describe, expect, it, vi } from "vitest";
import { act, fireEvent, render, renderHook, screen, within } from "@testing-library/react";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { InterviewQuestionManagementView } from "./interview-question-management-view";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, params?: Record<string, unknown>) =>
    params ? `${key} ${JSON.stringify(params)}` : key,
}));

beforeEach(() => act(() => toast.clear()));

describe("InterviewQuestionManagementView toasts", () => {
  it("toasts after confirming a delete", () => {
    const toasts = renderHook(() => useToasts());
    render(<InterviewQuestionManagementView basePath="/admin/interview-questions" />);
    fireEvent.click(screen.getAllByRole("button", { name: /^deleteQuestion/ })[0]!);
    fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "delete" }));
    expect(toasts.result.current.at(-1)).toMatchObject({ tone: "success" });
    expect(toasts.result.current.at(-1)?.message).toMatch(/^toast\.deleted/);
  });

  it("toasts the result count only on Enter", () => {
    const toasts = renderHook(() => useToasts());
    render(<InterviewQuestionManagementView basePath="/admin/interview-questions" />);
    const box = screen.getByLabelText("searchLabel");
    fireEvent.change(box, { target: { value: "zzzz-no-match" } });
    expect(toasts.result.current).toHaveLength(0);
    fireEvent.keyDown(box, { key: "Enter" });
    expect(toasts.result.current.at(-1)).toMatchObject({ tone: "info", message: "toast.searchEmpty" });
  });
});
