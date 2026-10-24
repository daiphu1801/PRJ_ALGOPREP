// Save result is a toast, not text next to the button.
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
} from "@testing-library/react";
import type { ReactElement } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { InterviewQuestionAuthoringView } from "./interview-question-authoring-view";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, params?: Record<string, unknown>) =>
    params ? `${key} ${JSON.stringify(params)}` : key,
}));

beforeEach(() => act(() => toast.clear()));

// The bank loads through TanStack Query, so each test renders inside a fresh client.
function renderWithQuery(ui: ReactElement) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>{ui}</QueryClientProvider>,
  );
}


describe("InterviewQuestionAuthoringView toasts", () => {
  it("toasts success on save and on soft delete", async () => {
    const toasts = renderHook(() => useToasts());
    renderWithQuery(
      <InterviewQuestionAuthoringView
        questionId="IQ-014"
        listHref="/admin/interview-questions"
      />,
    );
    fireEvent.click(await screen.findByRole("button", { name: "save" }));
    await waitFor(() =>
      expect(toasts.result.current.at(-1)).toMatchObject({
        tone: "success",
        message: "toast.saved",
      }),
    );
    fireEvent.click(screen.getByRole("button", { name: "softDelete" }));
    const confirm = screen
      .getAllByRole("button", { name: "softDelete" })
      .at(-1)!;
    fireEvent.click(confirm);
    expect(toasts.result.current.at(-1)).toMatchObject({
      tone: "success",
      message: "toast.softDeleted",
    });
  });
});

describe("InterviewQuestionAuthoringView classification", () => {
  it("starts a new question on the first topic and the first level of the admin-managed lists", () => {
    renderWithQuery(
      <InterviewQuestionAuthoringView
        questionId="new"
        listHref="/admin/interview-questions"
      />,
    );
    expect(screen.getByLabelText("levelLabel")).toHaveValue("EASY");
    expect(screen.getByLabelText("topicLabel")).toHaveValue("csTheory");
  });

  it("offers every level and shows the level of the question being edited", async () => {
    renderWithQuery(
      <InterviewQuestionAuthoringView
        questionId="IQ-014"
        listHref="/admin/interview-questions"
      />,
    );
    const level = await screen.findByLabelText("levelLabel");
    expect(level).toHaveValue("MEDIUM");
    expect(
      Array.from((level as HTMLSelectElement).options).map(
        (option) => option.text,
      ),
    ).toEqual(["Dễ", "Trung bình", "Khó"]);
  });
});
