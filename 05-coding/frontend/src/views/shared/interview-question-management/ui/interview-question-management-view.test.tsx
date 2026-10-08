// Toasts on the interview question list: delete result and explicit search.
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactElement } from "react";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { InterviewQuestionManagementView } from "./interview-question-management-view";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, params?: Record<string, unknown>) =>
    params ? `${key} ${JSON.stringify(params)}` : key,
}));

beforeEach(() => act(() => toast.clear()));

// The bank loads through TanStack Query, so every render waits for the toolbar to appear.
async function renderLoaded(ui: ReactElement) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const result = render(
    <QueryClientProvider client={client}>{ui}</QueryClientProvider>,
  );
  await screen.findByLabelText("searchLabel");
  return result;
}

describe("InterviewQuestionManagementView toasts", () => {
  it("toasts after confirming a delete", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(
      <InterviewQuestionManagementView basePath="/admin/interview-questions" />,
    );
    fireEvent.click(
      screen.getAllByRole("button", { name: /^deleteQuestion/ })[0]!,
    );
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: "delete",
      }),
    );
    expect(toasts.result.current.at(-1)).toMatchObject({ tone: "success" });
    expect(toasts.result.current.at(-1)?.message).toMatch(/^toast\.deleted/);
  });

  it("toasts the result count only on Enter", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(
      <InterviewQuestionManagementView basePath="/admin/interview-questions" />,
    );
    const box = screen.getByLabelText("searchLabel");
    fireEvent.change(box, { target: { value: "zzzz-no-match" } });
    expect(toasts.result.current).toHaveLength(0);
    fireEvent.keyDown(box, { key: "Enter" });
    expect(toasts.result.current.at(-1)).toMatchObject({
      tone: "info",
      message: "toast.searchEmpty",
    });
  });

  it("shows the topic and level managers to ADMIN only", async () => {
    const { unmount } = await renderLoaded(
      <InterviewQuestionManagementView
        basePath="/admin/interview-questions"
        canManageTopics
      />,
    );
    expect(
      screen.getByRole("button", { name: "manageLevels" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "manageTopics" }),
    ).toBeInTheDocument();
    unmount();
    await renderLoaded(
      <InterviewQuestionManagementView basePath="/instructor/interview-questions" />,
    );
    expect(
      screen.queryByRole("button", { name: "manageLevels" }),
    ).not.toBeInTheDocument();
  });

  it("level manager refuses deleting a level that questions still use, and a new level can be added", async () => {
    await renderLoaded(
      <InterviewQuestionManagementView
        basePath="/admin/interview-questions"
        canManageTopics
      />,
    );
    fireEvent.click(screen.getByRole("button", { name: "manageLevels" }));
    const dialog = screen.getByRole("dialog");
    expect(
      within(dialog).getAllByRole("button", {
        name: /^levelManager.deleteBlocked/,
      }),
    ).toHaveLength(3);
    fireEvent.change(within(dialog).getByLabelText("levelManager.newLabel"), {
      target: { value: "Cực khó" },
    });
    fireEvent.click(
      within(dialog).getByRole("button", { name: "levelManager.add" }),
    );
    expect(within(dialog).getByDisplayValue("Cực khó")).toBeInTheDocument();
  });
});

describe("InterviewQuestionManagementView list controls", () => {
  const codes = () =>
    screen
      .getAllByRole("row")
      .slice(1)
      .map((row) => within(row).getAllByRole("cell")[1]!.textContent);

  it("sorts by a column header and reverses on a second click", async () => {
    await renderLoaded(
      <InterviewQuestionManagementView basePath="/admin/interview-questions" />,
    );
    const header = screen.getByRole("button", { name: /^columnUsage/ });
    fireEvent.click(header);
    const ascending = codes();
    fireEvent.click(header);
    expect(codes()).not.toEqual(ascending);
    expect(
      screen.getByRole("columnheader", { name: /columnUsage/ }),
    ).toHaveAttribute("aria-sort", "descending");
  });

  it("offers a rows-per-page choice", async () => {
    await renderLoaded(
      <InterviewQuestionManagementView basePath="/admin/interview-questions" />,
    );
    const size = screen.getByRole("button", { name: /^pageSizeLabel/ });
    expect(size).toHaveTextContent("8");
    fireEvent.click(size);
    fireEvent.click(screen.getByRole("option", { name: "20" }));
    expect(
      screen.getByRole("button", { name: /^pageSizeLabel/ }),
    ).toHaveTextContent("20");
  });

  it("offers no duplicate action on a row", async () => {
    await renderLoaded(
      <InterviewQuestionManagementView basePath="/admin/interview-questions" />,
    );
    expect(
      screen.queryByRole("button", { name: /^duplicateQuestion/ }),
    ).toBeNull();
  });
});

describe("InterviewQuestionManagementView CSV import", () => {
  // jsdom's File has no text(); the dialog reads the file with it, as browsers do.
  const upload = (csv: string) =>
    fireEvent.change(screen.getByTestId("csv-input"), {
      target: {
        files: [
          Object.assign(
            new File([csv], "questions.csv", { type: "text/csv" }),
            {
              text: async () => csv,
            },
          ),
        ],
      },
    });

  it("imports the valid rows, lists the bad ones and shows the new questions in the list", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(
      <InterviewQuestionManagementView basePath="/admin/interview-questions" />,
    );
    fireEvent.click(screen.getByRole("button", { name: "importCsv" }));
    const dialog = screen.getByRole("dialog");
    upload(
      "topic,level,question,rubric\nLý thuyết CS,Dễ,Câu nhập từ CSV số một,\nkhông-có,Dễ,Dòng sai chủ đề,\n",
    );

    expect(
      await within(dialog).findByText(/^csvImport.summary/),
    ).toHaveTextContent('"imported":1,"total":2');
    expect(within(dialog).getByText(/^csvImport.rowError/)).toHaveTextContent(
      '"line":3',
    );
    expect(toasts.result.current.at(-1)).toMatchObject({ tone: "success" });

    fireEvent.click(
      within(dialog).getByRole("button", { name: "csvImport.close" }),
    );
    fireEvent.change(screen.getByLabelText("searchLabel"), {
      target: { value: "Câu nhập từ CSV số một" },
    });
    expect(
      await screen.findByText("Câu nhập từ CSV số một"),
    ).toBeInTheDocument();
  });

  it("rejects a file without the required columns and imports nothing", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(
      <InterviewQuestionManagementView basePath="/admin/interview-questions" />,
    );
    fireEvent.click(screen.getByRole("button", { name: "importCsv" }));
    upload("topic,question\nx,y\n");
    await waitFor(() =>
      expect(toasts.result.current.at(-1)).toMatchObject({ tone: "error" }),
    );
    expect(toasts.result.current.at(-1)?.message).toMatch(
      /^csvImport.fileError.missingColumns/,
    );
  });
});

describe("InterviewQuestionManagementView row selection", () => {
  it("selects rows, asks before a bulk delete, and drops the deleted rows from the selection", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(
      <InterviewQuestionManagementView basePath="/admin/interview-questions" />,
    );
    expect(screen.queryByRole("region")).toBeNull();

    fireEvent.click(
      screen.getAllByRole("checkbox", { name: /^selectRow/ })[0]!,
    );
    expect(screen.getByRole("region")).toHaveAccessibleName(/^selectionLabel/);

    fireEvent.click(screen.getByRole("button", { name: "bulk.delete" }));
    expect(toasts.result.current).toHaveLength(0); // only opened the dialog
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: "delete",
      }),
    );
    expect(toasts.result.current.at(-1)?.message).toMatch(
      /^toast\.bulkDeleted/,
    );
    expect(screen.queryByRole("region")).toBeNull();
  });

  it("warns when the filter hides some selected rows", async () => {
    await renderLoaded(
      <InterviewQuestionManagementView basePath="/admin/interview-questions" />,
    );
    fireEvent.click(
      screen.getAllByRole("checkbox", { name: /^selectRow/ })[0]!,
    );
    fireEvent.change(screen.getByLabelText("searchLabel"), {
      target: { value: "zzzz-no-match" },
    });
    fireEvent.click(screen.getByRole("button", { name: "bulk.delete" }));
    expect(
      within(screen.getByRole("dialog")).getByText(/^confirmBulkDeleteHidden/),
    ).toBeInTheDocument();
  });
});

describe("InterviewQuestionManagementView loading", () => {
  it("shows a skeleton first, then the list", async () => {
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    render(
      <QueryClientProvider client={client}>
        <InterviewQuestionManagementView basePath="/admin/interview-questions" />
      </QueryClientProvider>,
    );
    expect(document.querySelector('[aria-busy="true"]')).not.toBeNull();
    expect(screen.queryByLabelText("searchLabel")).toBeNull();
    await screen.findByLabelText("searchLabel");
    expect(document.querySelector('[aria-busy="true"]')).toBeNull();
  });
});
