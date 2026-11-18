// SHR0201 against the REAL api path (F2 #14 to #32): only the HTTP layer is faked, so the real modules,
// the error-code mapping and the real vi texts are what the assertions read.
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
// `useMockQuery` reads the flag from `mock-mode` directly, not through the barrel mocked above.
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: () => unknown, real: () => Promise<T>) => real(),
}));
const saveBlob = vi.hoisted(() => vi.fn());
vi.mock("@/shared/lib", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  saveBlob,
}));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));

// Import after the mocks.
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { ProblemManagementView } from "./problem-management-view";

const m = viMessages.problemManagement;
const errors = viMessages.apiErrors;
const BASE = "/problem-bank/authoring/problems";

const levels = [
  { id: "l1", code: "EASY", displayName: "Dễ", sortOrder: 1, problemCount: 2 },
  { id: "l2", code: "HARD", displayName: "Khó", sortOrder: 2, problemCount: 0 },
];
const topics = [
  { id: "t1", code: "ARRAY", name: "Mảng", sortOrder: 1, problemCount: 2 },
];

const row = (id: string, over: Record<string, unknown> = {}) => ({
  id,
  code: `P-${id}`,
  title: `Bài ${id}`,
  topicNames: ["Mảng"],
  levelId: "l1",
  levelCode: "EASY",
  levelName: "Dễ",
  status: "UNPUBLISHED",
  submissionCount: 3,
  assignedClassCount: 1,
  acRate: null,
  testcaseCount: 5,
  updatedAt: new Date(Date.now() - 3 * 86_400_000).toISOString(),
  ...over,
});

type Handler = Record<string, () => unknown>;

function serve(over: Handler = {}) {
  const handlers: Handler = {
    "/problem-bank/levels": () => ({ items: levels }),
    "/problem-bank/topics": () => ({ items: topics }),
    [BASE]: () => ({
      items: [row("1"), row("2", { acRate: 40.5, status: "PUBLISHED" })],
      page: { number: 1, size: 8, totalItems: 2, totalPages: 1 },
      totals: { total: 12, published: 7 },
    }),
    [`${BASE}/topic-distribution`]: () => ({
      totalProblemsWithTopic: 2,
      totalTopicsUsed: 1,
      items: [{ topicId: "t1", topicName: "Mảng", count: 2, percent: 100 }],
    }),
    [`${BASE}/attention`]: () => ({
      items: [
        { ruleCode: "NO_TESTCASE", count: 1 },
        { ruleCode: "LOW_AC", count: 0 },
        { ruleCode: "HIDDEN_FROM_LEARNERS", count: 0 },
        { ruleCode: "STALE_DRAFT", count: 0 },
      ],
    }),
    ...over,
  };
  apiMock.get.mockImplementation(async (path: string) => {
    const handler = handlers[path];
    if (!handler) throw new Error(`unexpected GET ${path}`);
    const value = handler();
    if (value instanceof Error) throw value;
    return value;
  });
}

const listCalls = () =>
  apiMock.get.mock.calls.filter(([path]) => path === BASE);

async function renderLoaded(canManageTopics = false) {
  render(
    withTestProviders(
      <ProblemManagementView
        basePath="/admin/problems"
        canManageTopics={canManageTopics}
      />,
    ),
  );
  await screen.findAllByRole("checkbox", { name: /^Chọn Bài/ });
}

const rowBoxes = () => screen.getAllByRole("checkbox", { name: /^Chọn Bài/ });

describe("ProblemManagementView with the real API", () => {
  beforeEach(() => {
    resetApiMock();
    saveBlob.mockClear();
    act(() => toast.clear());
    serve();
  });

  it("shows the server's rows, totals and side blocks, and never turns a missing AC rate into 0%", async () => {
    await renderLoaded();
    expect(
      screen.getByText(
        m.subtitle.replace("{total}", "12").replace("{published}", "7"),
      ),
    ).toBeInTheDocument();
    const rows = screen.getAllByRole("row");
    // Row 1 has no statistics row: "-", row 2 shows its rate.
    expect(within(rows[1]!).getByText("-")).toBeInTheDocument();
    expect(within(rows[2]!).getByText("40.5%")).toBeInTheDocument();
    expect(within(rows[1]!).getByText("3 ngày trước")).toBeInTheDocument();
    // The side blocks come from their own calls.
    expect(
      await screen.findByText(m.attention.rule.noTestcase.title),
    ).toBeInTheDocument();
    expect(await screen.findByText("2 · 100%")).toBeInTheDocument();
    // The first call carries the default query only.
    expect(listCalls()[0]![1]).toEqual({
      sort: "updatedAt",
      dir: "desc",
      page: 1,
      pageSize: 8,
    });
  });

  it("sends the chosen level as its id and clears the selection", async () => {
    await renderLoaded();
    fireEvent.click(rowBoxes()[0]!);
    fireEvent.click(
      screen.getByRole("button", {
        name: new RegExp(`^${m.difficultyFilterLabel}`),
      }),
    );
    fireEvent.click(screen.getByRole("option", { name: "Khó" }));
    await waitFor(() =>
      expect(listCalls().at(-1)![1]).toMatchObject({ level: "l2", page: 1 }),
    );
    expect(screen.queryByRole("region", { name: /^Đã chọn/ })).toBeNull();
  });

  it("shows the failure message when the list call fails, and keeps the filters", async () => {
    serve({ [BASE]: () => new ApiError("PRB-002", 403) });
    render(
      withTestProviders(<ProblemManagementView basePath="/admin/problems" />),
    );
    expect(await screen.findByText(m.loadFailed)).toBeInTheDocument();
    expect(screen.queryByRole("table")).toBeNull();
    expect(screen.getByLabelText(m.searchLabel)).toBeInTheDocument();
  });

  it("degrades one side block on its own when its call fails", async () => {
    serve({ [`${BASE}/attention`]: () => new ApiError("UNKNOWN_ERROR", 503) });
    await renderLoaded();
    expect(await screen.findByText(m.blockFailed)).toBeInTheDocument();
    // The table and the other block are untouched.
    expect(rowBoxes()).toHaveLength(2);
    expect(await screen.findByText("2 · 100%")).toBeInTheDocument();
  });

  it("bulk delete: reports the ones that went through and why the others did not, then reloads", async () => {
    const toasts = renderHook(() => useToasts());
    apiMock.post.mockResolvedValue({
      succeededIds: ["1"],
      failedItems: [
        { id: "2", code: "PRB-010", message: "gone", details: null },
      ],
    });
    await renderLoaded();
    fireEvent.click(screen.getByRole("checkbox", { name: m.selectAll }));
    fireEvent.click(screen.getByRole("button", { name: m.bulk.delete }));
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: m.delete,
      }),
    );
    await waitFor(() => expect(toasts.result.current).toHaveLength(1));
    expect(apiMock.post).toHaveBeenCalledWith(`${BASE}/bulk/delete`, {
      problemIds: ["1", "2"],
    });
    expect(toasts.result.current[0]).toMatchObject({
      tone: "warning",
      message: m.toast.bulkPartial
        .replace("{done}", "1")
        .replace("{failed}", "1")
        .replace("{reason}", errors["PRB-010"]),
    });
    // The list is fetched again after the write.
    await waitFor(() => expect(listCalls().length).toBeGreaterThan(1));
  });

  it("publish: a problem short of the checklist is named with its own reason", async () => {
    const toasts = renderHook(() => useToasts());
    apiMock.post.mockResolvedValue({
      succeededIds: [],
      failedItems: [
        { id: "1", code: "PRB-030", message: "x", details: { items: [] } },
      ],
    });
    await renderLoaded();
    fireEvent.click(rowBoxes()[0]!);
    fireEvent.click(screen.getByRole("button", { name: m.bulk.publish }));
    await waitFor(() => expect(toasts.result.current).toHaveLength(1));
    expect(apiMock.post).toHaveBeenCalledWith(`${BASE}/bulk/publish`, {
      problemIds: ["1"],
    });
    expect(toasts.result.current[0]!.message).toContain(errors["PRB-030"]);
  });

  it("change difficulty: a level gone from the server rejects the batch and says so", async () => {
    const toasts = renderHook(() => useToasts());
    apiMock.post.mockRejectedValue(new ApiError("PRB-039", 422));
    await renderLoaded();
    fireEvent.click(rowBoxes()[0]!);
    fireEvent.click(
      screen.getByRole("button", { name: m.bulk.changeDifficulty }),
    );
    const dialog = screen.getByRole("dialog");
    fireEvent.change(within(dialog).getByLabelText(m.bulkLevel.label), {
      target: { value: "HARD" },
    });
    fireEvent.click(
      within(dialog).getByRole("button", { name: m.bulkLevel.apply }),
    );
    await waitFor(() => expect(toasts.result.current).toHaveLength(1));
    expect(apiMock.post).toHaveBeenCalledWith(`${BASE}/bulk/difficulty`, {
      problemIds: ["1"],
      levelId: "l2",
    });
    expect(toasts.result.current[0]).toMatchObject({
      tone: "error",
      message: errors["PRB-039"],
    });
  });

  it("export: downloads the file under the server's name", async () => {
    const toasts = renderHook(() => useToasts());
    const blob = new Blob(["a"]);
    apiMock.downloadPost.mockResolvedValue({
      blob,
      filename: "problems-20261106.csv",
    });
    await renderLoaded();
    fireEvent.click(rowBoxes()[1]!);
    fireEvent.click(screen.getByRole("button", { name: m.bulk.exportCsv }));
    await waitFor(() => expect(saveBlob).toHaveBeenCalledTimes(1));
    expect(saveBlob).toHaveBeenCalledWith(blob, "problems-20261106.csv");
    expect(apiMock.downloadPost).toHaveBeenCalledWith(`${BASE}/export`, {
      problemIds: ["2"],
    });
    expect(toasts.result.current.at(-1)?.tone).toBe("success");
  });

  it("export: a refused download shows the translated reason", async () => {
    const toasts = renderHook(() => useToasts());
    apiMock.downloadPost.mockRejectedValue(new ApiError("PRB-002", 403));
    await renderLoaded();
    fireEvent.click(rowBoxes()[0]!);
    fireEvent.click(screen.getByRole("button", { name: m.bulk.exportCsv }));
    await waitFor(() => expect(toasts.result.current).toHaveLength(1));
    expect(saveBlob).not.toHaveBeenCalled();
    expect(toasts.result.current[0]!.message).toBe(errors["PRB-002"]);
  });

  describe("level manager (ADMIN)", () => {
    async function openLevels() {
      await renderLoaded(true);
      fireEvent.click(screen.getByRole("button", { name: m.manageLevels }));
      return screen.getByRole("dialog");
    }

    it("takes the usage count from the server and blocks deleting a level in use", async () => {
      const dialog = await openLevels();
      await within(dialog).findAllByLabelText(m.levelManager.nameLabel);
      expect(
        within(dialog).getByRole("button", {
          name: m.levelManager.deleteBlocked.replace("{count}", "2"),
        }),
      ).toHaveAttribute("aria-disabled", "true");
    });

    it("deletes an unused level through the api and reloads the catalogue", async () => {
      const toasts = renderHook(() => useToasts());
      apiMock.delete.mockResolvedValue(undefined);
      const dialog = await openLevels();
      const before = apiMock.get.mock.calls.filter(
        ([path]) => path === "/problem-bank/levels",
      ).length;
      fireEvent.click(
        await within(dialog).findByRole("button", { name: m.delete }),
      );
      await waitFor(() =>
        expect(apiMock.delete).toHaveBeenCalledWith("/problem-bank/levels/l2"),
      );
      await waitFor(() =>
        expect(toasts.result.current.at(-1)?.message).toBe(
          m.levelManager.done.remove,
        ),
      );
      await waitFor(() =>
        expect(
          apiMock.get.mock.calls.filter(
            ([path]) => path === "/problem-bank/levels",
          ).length,
        ).toBeGreaterThan(before),
      );
    });

    it("shows the server's reason when a delete is refused, and no success toast", async () => {
      const toasts = renderHook(() => useToasts());
      apiMock.delete.mockRejectedValue(new ApiError("PRB-032", 422));
      const dialog = await openLevels();
      fireEvent.click(
        await within(dialog).findByRole("button", { name: m.delete }),
      );
      await waitFor(() => expect(toasts.result.current).toHaveLength(1));
      expect(toasts.result.current[0]).toMatchObject({
        tone: "error",
        message: errors["PRB-032"],
      });
    });

    it("adds a level, refuses a duplicate name before any call, and translates a server clash", async () => {
      const toasts = renderHook(() => useToasts());
      apiMock.post.mockRejectedValue(new ApiError("PRB-022", 409));
      const dialog = await openLevels();
      await within(dialog).findAllByLabelText(m.levelManager.nameLabel);
      const field = within(dialog).getByLabelText(m.levelManager.newLabel);
      const add = within(dialog).getByRole("button", {
        name: m.levelManager.add,
      });

      fireEvent.change(field, { target: { value: " dễ " } });
      fireEvent.click(add);
      expect(toasts.result.current.at(-1)?.message).toBe(
        m.levelManager.error.duplicate,
      );
      expect(apiMock.post).not.toHaveBeenCalled();

      fireEvent.change(field, { target: { value: "Rất khó" } });
      fireEvent.click(add);
      await waitFor(() =>
        expect(toasts.result.current.at(-1)?.message).toBe(errors["PRB-022"]),
      );
      expect(apiMock.post).toHaveBeenCalledWith("/problem-bank/levels", {
        displayName: "Rất khó",
      });
    });

    it("reorders with the full list of ids in the new order", async () => {
      apiMock.put.mockResolvedValue(undefined);
      const dialog = await openLevels();
      await within(dialog).findAllByLabelText(m.levelManager.nameLabel);
      fireEvent.click(
        within(dialog).getAllByRole("button", {
          name: m.levelManager.moveDown,
        })[0]!,
      );
      await waitFor(() =>
        expect(apiMock.put).toHaveBeenCalledWith("/problem-bank/levels/order", {
          orderedIds: ["l2", "l1"],
        }),
      );
    });

    it("reorder refused with PRB-049 shows the reload hint", async () => {
      const toasts = renderHook(() => useToasts());
      apiMock.put.mockRejectedValue(new ApiError("PRB-049", 422));
      const dialog = await openLevels();
      await within(dialog).findAllByLabelText(m.levelManager.nameLabel);
      fireEvent.click(
        within(dialog).getAllByRole("button", {
          name: m.levelManager.moveDown,
        })[0]!,
      );
      await waitFor(() => expect(toasts.result.current).toHaveLength(1));
      expect(toasts.result.current[0]!.message).toBe(errors["PRB-049"]);
    });
  });

  describe("topic manager (ADMIN)", () => {
    it("blocks deleting a topic in use and renames through the api", async () => {
      apiMock.patch.mockResolvedValue(undefined);
      await renderLoaded(true);
      fireEvent.click(screen.getByRole("button", { name: m.manageTopics }));
      const dialog = screen.getByRole("dialog");
      expect(
        await within(dialog).findByRole("button", {
          name: m.topicManager.deleteBlocked.replace("{count}", "2"),
        }),
      ).toHaveAttribute("aria-disabled", "true");

      const name = within(dialog).getByLabelText(m.topicManager.nameLabel);
      fireEvent.change(name, { target: { value: "Mảng một chiều" } });
      fireEvent.click(
        within(dialog).getByRole("button", { name: m.topicManager.save }),
      );
      await waitFor(() =>
        expect(apiMock.patch).toHaveBeenCalledWith("/problem-bank/topics/t1", {
          name: "Mảng một chiều",
        }),
      );
    });
  });
});
