// The error path of the list: the query fails, the screen shows the failure message and no table.
import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { InterviewQuestionManagementView } from "./interview-question-management-view";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string) => key,
  useApiErrorMessage: () => () => "",
}));
const refetch = vi.fn();
vi.mock("../api", () => ({
  useAdminQuestionList: () => ({ isError: true, data: undefined, refetch }),
  useQuestionUsage: () => ({ data: undefined }),
  useRetireQuestions: () => ({ isPending: false, mutate: vi.fn() }),
  useImportQuestions: () => ({ isPending: false, mutateAsync: vi.fn() }),
  toImportRows: () => [],
}));

describe("InterviewQuestionManagementView load failure", () => {
  it("shows the load-failed message instead of the table", () => {
    render(
      <InterviewQuestionManagementView basePath="/admin/interview-questions" />,
    );
    expect(screen.getByText("loadFailed")).toBeInTheDocument();
    expect(screen.queryByRole("table")).toBeNull();
  });

  it("offers a retry button that refetches the list query", () => {
    refetch.mockReset();
    render(
      <InterviewQuestionManagementView basePath="/admin/interview-questions" />,
    );
    fireEvent.click(screen.getByRole("button", { name: "retry" }));
    expect(refetch).toHaveBeenCalledTimes(1);
  });
});
