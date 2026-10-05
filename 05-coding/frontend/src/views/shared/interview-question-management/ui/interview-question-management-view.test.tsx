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

  it("shows the topic and level managers to ADMIN only", () => {
    const { unmount } = render(
      <InterviewQuestionManagementView basePath="/admin/interview-questions" canManageTopics />,
    );
    expect(screen.getByRole("button", { name: "manageLevels" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "manageTopics" })).toBeInTheDocument();
    unmount();
    render(<InterviewQuestionManagementView basePath="/instructor/interview-questions" />);
    expect(screen.queryByRole("button", { name: "manageLevels" })).not.toBeInTheDocument();
  });

  it("level manager refuses deleting a level that questions still use, and a new level can be added", () => {
    render(<InterviewQuestionManagementView basePath="/admin/interview-questions" canManageTopics />);
    fireEvent.click(screen.getByRole("button", { name: "manageLevels" }));
    const dialog = screen.getByRole("dialog");
    expect(within(dialog).getAllByRole("button", { name: /^levelManager.deleteBlocked/ })).toHaveLength(3);
    fireEvent.change(within(dialog).getByLabelText("levelManager.newLabel"), { target: { value: "Cực khó" } });
    fireEvent.click(within(dialog).getByRole("button", { name: "levelManager.add" }));
    expect(within(dialog).getByDisplayValue("Cực khó")).toBeInTheDocument();
  });
});

describe("InterviewQuestionManagementView list controls", () => {
  const codes = () =>
    screen.getAllByRole("row").slice(1).map((row) => within(row).getAllByRole("cell")[1]!.textContent);

  it("sorts by a column header and reverses on a second click", () => {
    render(<InterviewQuestionManagementView basePath="/admin/interview-questions" />);
    const header = screen.getByRole("button", { name: /^columnUsage/ });
    fireEvent.click(header);
    const ascending = codes();
    fireEvent.click(header);
    expect(codes()).not.toEqual(ascending);
    expect(screen.getByRole("columnheader", { name: /columnUsage/ })).toHaveAttribute("aria-sort", "descending");
  });

  it("offers a rows-per-page choice", () => {
    render(<InterviewQuestionManagementView basePath="/admin/interview-questions" />);
    const size = screen.getByRole("button", { name: /^pageSizeLabel/ });
    expect(size).toHaveTextContent("8");
    fireEvent.click(size);
    fireEvent.click(screen.getByRole("option", { name: "20" }));
    expect(screen.getByRole("button", { name: /^pageSizeLabel/ })).toHaveTextContent("20");
  });

  it("does not claim success for the stubbed duplicate action", () => {
    const toasts = renderHook(() => useToasts());
    render(<InterviewQuestionManagementView basePath="/admin/interview-questions" />);
    fireEvent.click(screen.getAllByRole("button", { name: /^duplicateQuestion/ })[0]!);
    expect(toasts.result.current.at(-1)).toMatchObject({ tone: "info" });
    expect(toasts.result.current.at(-1)?.message).toMatch(/^toast\.notWired/);
  });
});

describe("InterviewQuestionManagementView row selection", () => {
  it("selects rows, asks before a bulk delete, and drops the deleted rows from the selection", () => {
    const toasts = renderHook(() => useToasts());
    render(<InterviewQuestionManagementView basePath="/admin/interview-questions" />);
    expect(screen.queryByRole("region")).toBeNull();

    fireEvent.click(screen.getAllByRole("checkbox", { name: /^selectRow/ })[0]!);
    expect(screen.getByRole("region")).toHaveAccessibleName(/^selectionLabel/);

    fireEvent.click(screen.getByRole("button", { name: "bulk.delete" }));
    expect(toasts.result.current).toHaveLength(0); // only opened the dialog
    fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "delete" }));
    expect(toasts.result.current.at(-1)?.message).toMatch(/^toast\.bulkDeleted/);
    expect(screen.queryByRole("region")).toBeNull();
  });

  it("warns when the filter hides some selected rows", () => {
    render(<InterviewQuestionManagementView basePath="/admin/interview-questions" />);
    fireEvent.click(screen.getAllByRole("checkbox", { name: /^selectRow/ })[0]!);
    fireEvent.change(screen.getByLabelText("searchLabel"), { target: { value: "zzzz-no-match" } });
    fireEvent.click(screen.getByRole("button", { name: "bulk.delete" }));
    expect(within(screen.getByRole("dialog")).getByText(/^confirmBulkDeleteHidden/)).toBeInTheDocument();
  });

  it("does not claim success for the stubbed bulk duplicate", () => {
    const toasts = renderHook(() => useToasts());
    render(<InterviewQuestionManagementView basePath="/admin/interview-questions" />);
    fireEvent.click(screen.getAllByRole("checkbox", { name: /^selectRow/ })[0]!);
    fireEvent.click(screen.getByRole("button", { name: "bulk.duplicate" }));
    expect(toasts.result.current.at(-1)).toMatchObject({ tone: "info" });
  });
});
