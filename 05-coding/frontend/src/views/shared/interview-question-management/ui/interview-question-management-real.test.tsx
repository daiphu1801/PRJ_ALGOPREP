// SHR0301 against the REAL api path (F6 #19, #23, #24): only the HTTP layer is faked, so the real
// modules, the error mapping and the real vi texts are what the assertions read. Mock-data behaviour
// stays in interview-question-management-view.test.tsx.
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../messages/vi.json";
import { ApiError } from "@/shared/api";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);
// `useQuestionUsage` reads the flag from `mock-mode` directly, not through the barrel mocked above.
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: () => unknown, real: () => Promise<T>) => real(),
}));
// The topic and level lists belong to another slice; this screen only reads them.
vi.mock("@/entities/interview-question", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  useInterviewTopics: () => [
    {
      id: "t-1",
      key: "CS_THEORY",
      label: "Lý thuyết CS",
      usesStarFramework: false,
    },
  ],
  useInterviewLevels: () => [
    { id: "l-1", key: "MEDIUM", label: "Trung bình", tone: "warn" },
  ],
}));

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { InterviewQuestionManagementView } from "./interview-question-management-view";

const m = viMessages.interviewQuestionManagement;
const apiErrors = viMessages.apiErrors as Record<string, string>;
const BASE = "/interview-bank/admin/questions";
const A = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
const B = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb";

const row = (
  id: string,
  displayNo: number,
  over: Record<string, unknown> = {},
) => ({
  id,
  displayNo,
  level: { code: "MEDIUM", displayName: "Trung bình" },
  topic: { code: "CS_THEORY", displayName: "Lý thuyết CS" },
  title: `Câu hỏi số ${displayNo}`,
  followUpCount: 2,
  rubricCount: 3,
  usageCount: 5,
  averageScorePercent: null,
  ...over,
});

const listDto = (
  items = [row(A, 14, { averageScorePercent: 81.4 }), row(B, 21)],
) => ({
  items,
  page: { number: 1, size: 8, totalItems: items.length, totalPages: 1 },
  meta: { shown: items.length, total: 40 },
});

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

async function open() {
  render(
    withTestProviders(
      <InterviewQuestionManagementView basePath="/admin/interview-questions" />,
    ),
  );
  await screen.findByLabelText(m.searchLabel);
}

const lastListParams = () =>
  apiMock.get.mock.calls.filter(([path]) => path === BASE).at(-1)![1];

async function confirmDelete() {
  fireEvent.click(
    within(screen.getByRole("dialog")).getByRole("button", { name: m.delete }),
  );
}

describe("InterviewQuestionManagementView with the real API", () => {
  beforeEach(() => {
    resetApiMock();
    apiMock.get.mockResolvedValue(listDto());
  });

  it("asks the server for page 1 of 8 with no filter and shows the rows with their IQ label", async () => {
    await open();
    expect(apiMock.get).toHaveBeenCalledWith(BASE, { page: 1, pageSize: 8 });
    expect(screen.getByText("IQ-014")).toBeInTheDocument();
    expect(screen.getByText("IQ-021")).toBeInTheDocument();
    // 81.4 rounds to 81 percent; null stays a dash, never 0.
    expect(screen.getByText("81%")).toBeInTheDocument();
    expect(screen.getByText("-")).toBeInTheDocument();
    // Row links use the UUID, not the label.
    expect(screen.getByRole("link", { name: "Câu hỏi số 14" })).toHaveAttribute(
      "href",
      `/admin/interview-questions/${A}`,
    );
    expect(
      screen.getByRole("link", {
        name: m.editQuestion.replace("{code}", "IQ-014"),
      }),
    ).toHaveAttribute("href", `/admin/interview-questions/${A}/edit`);
  });

  it("sends the sort column and direction, and the filter codes, then starts again from page 1", async () => {
    await open();
    fireEvent.click(
      screen.getByRole("button", { name: new RegExp(`^${m.columnUsage}`) }),
    );
    await waitFor(() =>
      expect(lastListParams()).toEqual({
        page: 1,
        pageSize: 8,
        sort: "usageCount",
        dir: "asc",
      }),
    );
    fireEvent.click(
      screen.getByRole("button", { name: new RegExp(`^${m.columnUsage}`) }),
    );
    await waitFor(() =>
      expect(lastListParams()).toMatchObject({ dir: "desc" }),
    );
  });

  it("debounces the search text into one q parameter and reports the count on Enter", async () => {
    const toasts = watch();
    await open();
    apiMock.get.mockClear();
    const box = screen.getByLabelText(m.searchLabel);
    fireEvent.change(box, { target: { value: "ha" } });
    fireEvent.change(box, { target: { value: "hash" } });
    await waitFor(() => expect(lastListParams()).toMatchObject({ q: "hash" }));
    expect(
      apiMock.get.mock.calls.filter(([path]) => path === BASE),
    ).toHaveLength(1);
    fireEvent.keyDown(box, { key: "Enter" });
    expect(toasts().at(-1)).toEqual([
      "info",
      m.toast.searchResult.replace("{count}", "2"),
    ]);
  });

  it("shows the subtitle total, and a different empty text for an empty bank than for a filter", async () => {
    apiMock.get.mockResolvedValue({
      ...listDto([]),
      meta: { shown: 0, total: 0 },
    });
    await open();
    expect(screen.getByText(m.emptyBank)).toBeInTheDocument();
  });

  it("shows the load failure instead of the table when the first read fails", async () => {
    apiMock.get.mockRejectedValue(new ApiError("INB-301", 403));
    render(
      withTestProviders(
        <InterviewQuestionManagementView basePath="/admin/interview-questions" />,
      ),
    );
    expect(await screen.findByText(m.loadFailed)).toBeInTheDocument();
    expect(screen.queryByRole("table")).toBeNull();
  });

  it("retry after a failed first read reads the list again and then shows the table", async () => {
    apiMock.get.mockRejectedValueOnce(new ApiError("INB-503", 503));
    render(
      withTestProviders(
        <InterviewQuestionManagementView basePath="/admin/interview-questions" />,
      ),
    );
    expect(await screen.findByText(m.loadFailed)).toBeInTheDocument();
    const calls = apiMock.get.mock.calls.length;
    apiMock.get.mockResolvedValue(listDto());
    fireEvent.click(screen.getByRole("button", { name: m.retry }));
    expect(await screen.findByLabelText(m.searchLabel)).toBeInTheDocument();
    expect(apiMock.get.mock.calls.length).toBeGreaterThan(calls);
    expect(screen.getByText("IQ-014")).toBeInTheDocument();
    expect(screen.queryByText(m.loadFailed)).toBeNull();
  });

  it("clears the selection when the sort changes", async () => {
    await open();
    fireEvent.click(
      screen.getAllByRole("checkbox", { name: /^Chọn|select/i })[1]!,
    );
    expect(screen.getByRole("region")).toBeInTheDocument();
    fireEvent.click(
      screen.getByRole("button", { name: new RegExp(`^${m.columnUsage}`) }),
    );
    await waitFor(() => expect(screen.queryByRole("region")).toBeNull());
  });

  describe("delete", () => {
    it("retires one row by its UUID, toasts the code and refreshes the list", async () => {
      apiMock.post.mockResolvedValue({ retiredIds: [A], failedItems: [] });
      const toasts = watch();
      await open();
      apiMock.get.mockClear();
      fireEvent.click(
        screen.getByRole("button", {
          name: m.deleteQuestion.replace("{code}", "IQ-014"),
        }),
      );
      await confirmDelete();
      await waitFor(() =>
        expect(apiMock.post).toHaveBeenCalledWith(`${BASE}/bulk/delete`, {
          ids: [A],
        }),
      );
      await waitFor(() =>
        expect(toasts().at(-1)).toEqual([
          "success",
          m.toast.deleted.replace("{code}", "IQ-014"),
        ]),
      );
      // Invalidation: the list is read again after the write.
      await waitFor(() =>
        expect(apiMock.get.mock.calls.some(([path]) => path === BASE)).toBe(
          true,
        ),
      );
    });

    it("bulk delete sends every selected UUID and drops them from the selection", async () => {
      apiMock.post.mockResolvedValue({ retiredIds: [A, B], failedItems: [] });
      const toasts = watch();
      await open();
      fireEvent.click(
        screen.getByRole("checkbox", { name: new RegExp(m.selectAll) }),
      );
      fireEvent.click(screen.getByRole("button", { name: m.bulk.delete }));
      await confirmDelete();
      await waitFor(() =>
        expect(apiMock.post).toHaveBeenCalledWith(`${BASE}/bulk/delete`, {
          ids: [A, B],
        }),
      );
      await waitFor(() =>
        expect(toasts().at(-1)).toEqual([
          "success",
          m.toast.bulkDeleted.replace("{count}", "2"),
        ]),
      );
      expect(screen.queryByRole("region")).toBeNull();
    });

    it("a partial success warns with the reason and keeps the failed id selected", async () => {
      apiMock.post.mockResolvedValue({
        retiredIds: [A],
        failedItems: [{ id: B, errorCode: "INB-010" }],
      });
      const toasts = watch();
      await open();
      fireEvent.click(
        screen.getByRole("checkbox", { name: new RegExp(m.selectAll) }),
      );
      fireEvent.click(screen.getByRole("button", { name: m.bulk.delete }));
      await confirmDelete();
      await waitFor(() =>
        expect(toasts().at(-1)).toEqual([
          "warning",
          m.toast.bulkPartial
            .replace("{done}", "1")
            .replace("{failed}", "1")
            .replace("{reason}", apiErrors["INB-010"]!),
        ]),
      );
      // The id that failed is still selected.
      expect(screen.getByRole("region")).toHaveAccessibleName(
        m.selectionLabel.replace("{count}", "1"),
      );
    });

    it.each([
      ["INB-301", 403],
      ["INB-V02", 400],
      ["INB-503", 503],
    ])(
      "shows the text of %s when the call fails and keeps the selection",
      async (code, status) => {
        apiMock.post.mockRejectedValue(new ApiError(code, status));
        const toasts = watch();
        await open();
        fireEvent.click(
          screen.getByRole("checkbox", { name: new RegExp(m.selectAll) }),
        );
        fireEvent.click(screen.getByRole("button", { name: m.bulk.delete }));
        await confirmDelete();
        await waitFor(() =>
          expect(toasts().at(-1)).toEqual(["error", apiErrors[code]]),
        );
        expect(screen.getByRole("region")).toBeInTheDocument();
      },
    );

    it("an unknown failure shows the generic text", async () => {
      apiMock.post.mockRejectedValue(new Error("boom"));
      const toasts = watch();
      await open();
      fireEvent.click(
        screen.getByRole("button", {
          name: m.deleteQuestion.replace("{code}", "IQ-014"),
        }),
      );
      await confirmDelete();
      await waitFor(() =>
        expect(toasts().at(-1)).toEqual(["error", apiErrors.UNKNOWN_ERROR]),
      );
    });
  });

  describe("CSV import", () => {
    const upload = (csv: string) =>
      fireEvent.change(screen.getByTestId("csv-input"), {
        target: {
          files: [
            Object.assign(new File([csv], "q.csv", { type: "text/csv" }), {
              text: async () => csv,
            }),
          ],
        },
      });
    const CSV =
      "topic,level,question,rubric,follow_ups\n" +
      "Lý thuyết CS,Trung bình,Câu một,Ý một:60|Ý hai:40,Sâu một|Sâu hai\n" +
      "không-có,Trung bình,Dòng lỗi,,\n" +
      "CS_THEORY,MEDIUM,Câu ba,,\n";

    async function openImport() {
      await open();
      fireEvent.click(screen.getByRole("button", { name: m.importCsv }));
      return screen.getByRole("dialog");
    }

    it("sends only the client-valid rows with their physical file line, then shows the server report", async () => {
      apiMock.post.mockResolvedValue({
        importedCount: 1,
        skippedRows: [{ line: 4, reasonCode: "duplicate", detail: null }],
      });
      const toasts = watch();
      const dialog = await openImport();
      upload(CSV);
      await waitFor(() =>
        expect(apiMock.post).toHaveBeenCalledWith(`${BASE}/import`, {
          rows: [
            {
              line: 2,
              topic: "CS_THEORY",
              level: "MEDIUM",
              question: "Câu một",
              rubric: [
                { criterionCode: "Ý một", weightPercent: 60 },
                { criterionCode: "Ý hai", weightPercent: 40 },
              ],
              followUps: ["Sâu một", "Sâu hai"],
            },
            {
              line: 4,
              topic: "CS_THEORY",
              level: "MEDIUM",
              question: "Câu ba",
              rubric: [],
            },
          ],
        }),
      );
      // Server imported 1 of the 3 data rows; the client rejected line 3, the server skipped line 4.
      expect(
        await within(dialog).findByText(
          m.csvImport.summary
            .replace("{imported}", "1")
            .replace("{total}", "3"),
        ),
      ).toBeInTheDocument();
      const items = within(dialog)
        .getAllByRole("listitem")
        .map((item) => item.textContent);
      expect(items).toContain(
        m.csvImport.rowError
          .replace("{line}", "3")
          .replace(
            "{reason}",
            m.csvImport.reason.unknownTopic.replace("{detail}", "không-có"),
          ),
      );
      expect(items).toContain(
        m.csvImport.rowError
          .replace("{line}", "4")
          .replace("{reason}", m.csvImport.reason.duplicate),
      );
      expect(toasts().at(-1)).toEqual([
        "success",
        m.csvImport.toast.replace("{count}", "1"),
      ]);
    });

    it("reports invalidField from the server and gives no success toast when nothing was imported", async () => {
      apiMock.post.mockResolvedValue({
        importedCount: 0,
        skippedRows: [
          { line: 2, reasonCode: "invalidField", detail: null },
          { line: 4, reasonCode: "rubricSum", detail: null },
        ],
      });
      const toasts = watch();
      const dialog = await openImport();
      upload(CSV);
      expect(
        await within(dialog).findByText(
          m.csvImport.summary
            .replace("{imported}", "0")
            .replace("{total}", "3"),
        ),
      ).toBeInTheDocument();
      expect(
        within(dialog).getByText(
          m.csvImport.rowError
            .replace("{line}", "2")
            .replace("{reason}", m.csvImport.reason.invalidField),
        ),
      ).toBeInTheDocument();
      expect(toasts()).toEqual([]);
    });

    it("never calls the server for a file that fails as a whole", async () => {
      const toasts = watch();
      await openImport();
      upload("topic,question\nx,y\n");
      await waitFor(() => expect(toasts()).toHaveLength(1));
      expect(apiMock.post).not.toHaveBeenCalled();
    });

    it.each([
      ["INB-V15", 400],
      ["INB-301", 403],
      ["INB-V17", 413],
      ["INB-503", 503],
    ])("shows the text of %s and leaves no report", async (code, status) => {
      apiMock.post.mockRejectedValue(new ApiError(code, status));
      const toasts = watch();
      const dialog = await openImport();
      upload(CSV);
      await waitFor(() =>
        expect(toasts().at(-1)).toEqual(["error", apiErrors[code]]),
      );
      expect(within(dialog).queryByRole("status")).toBeNull();
    });
  });
});
