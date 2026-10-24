// Smoke test (PROTOTYPE lane): known code renders with an /edit link, unknown code shows not-found.
import type { ReactElement } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { InterviewQuestionInfoView } from "./interview-question-info-view";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, params?: Record<string, unknown>) =>
    params ? `${key} ${JSON.stringify(params)}` : key,
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

describe("InterviewQuestionInfoView", () => {
  it("shows the question and links the edit button to the edit route", async () => {
    renderWithQuery(
      <InterviewQuestionInfoView
        questionId="IQ-014"
        basePath="/admin/interview-questions"
      />,
    );

    expect(
      await screen.findByText(/Hash table xử lý collision bằng cách nào\?/),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "edit" })).toHaveAttribute(
      "href",
      "/admin/interview-questions/IQ-014/edit",
    );
  });

  it("shows the not-found state for an unknown code instead of crashing", async () => {
    renderWithQuery(
      <InterviewQuestionInfoView
        questionId="IQ-nope"
        basePath="/admin/interview-questions"
      />,
    );

    expect(await screen.findByText(/notFoundBody/)).toBeInTheDocument();
  });
});
