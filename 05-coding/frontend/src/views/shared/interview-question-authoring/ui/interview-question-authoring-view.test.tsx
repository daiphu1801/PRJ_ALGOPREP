// Save result is a toast, not text next to the button.
import { beforeEach, describe, expect, it, vi } from "vitest";
import { act, fireEvent, render, renderHook, screen, waitFor } from "@testing-library/react";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { InterviewQuestionAuthoringView } from "./interview-question-authoring-view";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, params?: Record<string, unknown>) =>
    params ? `${key} ${JSON.stringify(params)}` : key,
}));

beforeEach(() => act(() => toast.clear()));

describe("InterviewQuestionAuthoringView toasts", () => {
  it("toasts success on save and on soft delete", async () => {
    const toasts = renderHook(() => useToasts());
    render(<InterviewQuestionAuthoringView questionId="IQ-014" listHref="/admin/interview-questions" />);
    fireEvent.click(screen.getByRole("button", { name: "save" }));
    await waitFor(() =>
      expect(toasts.result.current.at(-1)).toMatchObject({ tone: "success", message: "toast.saved" }),
    );
    fireEvent.click(screen.getByRole("button", { name: "softDelete" }));
    const confirm = screen.getAllByRole("button", { name: "softDelete" }).at(-1)!;
    fireEvent.click(confirm);
    expect(toasts.result.current.at(-1)).toMatchObject({ tone: "success", message: "toast.softDeleted" });
  });
});
