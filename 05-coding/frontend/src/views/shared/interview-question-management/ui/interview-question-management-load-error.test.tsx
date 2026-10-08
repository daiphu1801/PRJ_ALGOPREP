// The error path of the list: the query fails, the screen shows the failure message and no table.
import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { InterviewQuestionManagementView } from "./interview-question-management-view";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string) => key,
}));
vi.mock("../api", () => ({
  useInterviewQuestionPage: () => ({ isError: true, data: undefined }),
}));

describe("InterviewQuestionManagementView load failure", () => {
  it("shows the load-failed message instead of the table", () => {
    render(
      <InterviewQuestionManagementView basePath="/admin/interview-questions" />,
    );
    expect(screen.getByText("loadFailed")).toBeInTheDocument();
    expect(screen.queryByRole("table")).toBeNull();
  });
});
