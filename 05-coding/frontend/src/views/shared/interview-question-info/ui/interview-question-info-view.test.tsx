// Smoke test (PROTOTYPE lane): known code renders with an /edit link, unknown code shows not-found.
import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { InterviewQuestionInfoView } from "./interview-question-info-view";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, params?: Record<string, unknown>) =>
    params ? `${key} ${JSON.stringify(params)}` : key,
}));

describe("InterviewQuestionInfoView", () => {
  it("shows the question and links the edit button to the edit route", () => {
    render(
      <InterviewQuestionInfoView
        questionId="IQ-014"
        basePath="/admin/interview-questions"
      />,
    );

    expect(
      screen.getByText(/Hash table xử lý collision bằng cách nào\?/),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "edit" })).toHaveAttribute(
      "href",
      "/admin/interview-questions/IQ-014/edit",
    );
  });

  it("shows the not-found state for an unknown code instead of crashing", () => {
    render(
      <InterviewQuestionInfoView
        questionId="IQ-nope"
        basePath="/admin/interview-questions"
      />,
    );

    expect(screen.getByText(/notFoundBody/)).toBeInTheDocument();
  });
});
