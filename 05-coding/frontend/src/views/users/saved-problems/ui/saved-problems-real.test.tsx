// USR0103 against the REAL api path (F2 #8 to #10): only the HTTP layer is faked, so the real modules,
// the error mapping and the real vi texts are what the assertions read.
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

// Import after the mocks.
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { SavedProblemsView } from "./saved-problems-view";

const m = viMessages.savedProblems;
const errors = viMessages.apiErrors;
const BASE = "/problem-bank/me/bookmarks";

const levels = [
  {
    id: "l1",
    code: "EASY",
    displayName: "Dễ",
    sortOrder: 1,
    problemCount: null,
  },
  {
    id: "l2",
    code: "HARD",
    displayName: "Khó",
    sortOrder: 2,
    problemCount: null,
  },
];

const item = (id: string, over: Record<string, unknown> = {}) => ({
  problemId: id,
  code: `P-${id}`,
  title: `Bài ${id}`,
  levelCode: "EASY",
  levelDisplayName: "Dễ",
  topicNames: ["Mảng"],
  solveState: "SOLVED",
  note: `note ${id}`,
  savedAt: "2026-11-01T03:00:00Z",
  withdrawn: false,
  ...over,
});

type Handler = (params?: Record<string, unknown>) => unknown;

function serve(list: Handler) {
  apiMock.get.mockImplementation(
    async (path: string, params?: Record<string, unknown>) => {
      const value =
        path === "/problem-bank/levels"
          ? { items: levels }
          : path === BASE
            ? list(params)
            : undefined;
      if (value === undefined) throw new Error(`unexpected GET ${path}`);
      if (value instanceof Error) throw value;
      return value;
    },
  );
}

const twoRows = () => ({
  items: [
    item("1"),
    item("2", { solveState: null, withdrawn: true, note: "" }),
  ],
  page: { number: 1, size: 20, totalItems: 2, totalPages: 1 },
  meta: { savedTotal: 2, solvedCount: 1, filteredTotal: 2 },
});

const listCalls = () =>
  apiMock.get.mock.calls.filter(([path]) => path === BASE);

// The undo offer is a toast action; this stand-in draws just the action buttons of the live toasts.
function ToastActions() {
  return (
    <>
      {useToasts().map((entry) =>
        entry.action ? (
          <button key={entry.id} type="button" onClick={entry.action.onClick}>
            {entry.action.label}
          </button>
        ) : null,
      )}
    </>
  );
}

async function renderLoaded() {
  render(
    withTestProviders(
      <>
        <ToastActions />
        <SavedProblemsView />
      </>,
    ),
  );
  await screen.findByText("Bài 1");
}

describe("SavedProblemsView with the real API", () => {
  beforeEach(() => {
    resetApiMock();
    localStorage.clear();
    act(() => toast.clear());
    serve(twoRows);
  });

  it("draws the server's rows and totals; a row with no solve state shows 'Chưa rõ', not 'Chưa làm'", async () => {
    await renderLoaded();
    expect(screen.getByText("2 bài đã lưu, 1 bài đã giải")).toBeInTheDocument();
    const rows = screen.getAllByRole("row");
    expect(within(rows[1]!).getByText("Đã giải")).toBeInTheDocument();
    expect(within(rows[2]!).getByText(m.state.unknown)).toBeInTheDocument();
    expect(within(rows[2]!).queryByText(m.state.todo)).toBeNull();
    expect(within(rows[2]!).getByText(m.table.withdrawn)).toBeInTheDocument();
    expect(within(rows[1]!).getByText("01/11/2026")).toBeInTheDocument();
    expect(listCalls()[0]![1]).toEqual({
      sort: "savedAt",
      dir: "desc",
      page: 1,
    });
  });

  it("degraded solve state: no solved count in the header, a notice, and no invented status", async () => {
    serve(() => ({
      items: [item("1", { solveState: null })],
      page: { number: 1, size: 20, totalItems: 1, totalPages: 1 },
      meta: { savedTotal: 1, solvedCount: null, filteredTotal: 1 },
    }));
    await renderLoaded();
    expect(screen.getByText("1 bài đã lưu")).toBeInTheDocument();
    expect(screen.queryByText(/bài đã giải/)).toBeNull();
    expect(screen.getByText(m.degraded.solveState)).toBeInTheDocument();
  });

  it("an empty bookmark list shows the empty state, not an empty table", async () => {
    serve(() => ({
      items: [],
      page: { number: 1, size: 20, totalItems: 0, totalPages: 1 },
      meta: { savedTotal: 0, solvedCount: 0, filteredTotal: 0 },
    }));
    render(withTestProviders(<SavedProblemsView />));
    expect(await screen.findByText(m.emptyAll)).toBeInTheDocument();
    expect(screen.queryByRole("table")).toBeNull();
  });

  it("sends the status, the level and a debounced search to the server and goes back to page 1", async () => {
    await renderLoaded();
    fireEvent.click(
      screen.getByRole("button", {
        name: new RegExp(`^${m.filter.statusLabel}`),
      }),
    );
    fireEvent.click(screen.getByRole("option", { name: m.state.attempted }));
    await waitFor(() =>
      expect(listCalls().at(-1)![1]).toMatchObject({
        status: "ATTEMPTED",
        page: 1,
      }),
    );
    fireEvent.click(
      screen.getByRole("button", {
        name: new RegExp(`^${m.filter.difficultyLabel}`),
      }),
    );
    fireEvent.click(screen.getByRole("option", { name: "Khó" }));
    fireEvent.change(screen.getByLabelText(m.filter.searchLabel), {
      target: { value: " dp " },
    });
    await waitFor(() =>
      expect(listCalls().at(-1)![1]).toMatchObject({
        q: "dp",
        status: "ATTEMPTED",
        difficulty: "HARD",
      }),
    );
  });

  it("the list failing shows the retry block", async () => {
    serve(() => new ApiError("UNKNOWN_ERROR", 503));
    render(withTestProviders(<SavedProblemsView />));
    expect(
      await screen.findByText(viMessages.common.error),
    ).toBeInTheDocument();
    expect(screen.queryByRole("table")).toBeNull();
  });

  it("unsave deletes the bookmark, reloads, and undo restores it with its note", async () => {
    apiMock.delete.mockResolvedValue(undefined);
    apiMock.put.mockResolvedValue({
      problemId: "1",
      note: "note 1",
      savedAt: "x",
    });
    await renderLoaded();
    fireEvent.click(screen.getAllByText(m.table.btnUnsave)[0]!);
    await waitFor(() =>
      expect(apiMock.delete).toHaveBeenCalledWith(`${BASE}/1`),
    );
    await waitFor(() => expect(listCalls().length).toBeGreaterThan(1));
    fireEvent.click(await screen.findByText(m.undo.button));
    await waitFor(() =>
      expect(apiMock.put).toHaveBeenCalledWith(`${BASE}/1`, {
        note: "note 1",
        createIfMissing: true,
      }),
    );
  });

  it("unsave failing shows the translated reason and offers no undo", async () => {
    const toasts = renderHook(() => useToasts());
    apiMock.delete.mockRejectedValue(new ApiError("UNKNOWN_ERROR", 503));
    await renderLoaded();
    fireEvent.click(screen.getAllByText(m.table.btnUnsave)[0]!);
    await waitFor(() => expect(toasts.result.current).toHaveLength(1));
    expect(toasts.result.current[0]).toMatchObject({
      tone: "error",
      message: errors.UNKNOWN_ERROR,
    });
    expect(screen.queryByText(m.undo.button)).toBeNull();
  });

  it("undo refused because the problem was withdrawn meanwhile says so (PRB-010)", async () => {
    const toasts = renderHook(() => useToasts());
    apiMock.delete.mockResolvedValue(undefined);
    apiMock.put.mockRejectedValue(new ApiError("PRB-010", 404));
    await renderLoaded();
    fireEvent.click(screen.getAllByText(m.table.btnUnsave)[0]!);
    fireEvent.click(await screen.findByText(m.undo.button));
    await waitFor(() =>
      expect(toasts.result.current.at(-1)).toMatchObject({
        tone: "error",
        message: errors["PRB-010"],
      }),
    );
  });

  it("saving a note sends the trimmed text without recreating the bookmark", async () => {
    const toasts = renderHook(() => useToasts());
    apiMock.put.mockResolvedValue({
      problemId: "1",
      note: "mới",
      savedAt: "x",
    });
    await renderLoaded();
    const row = screen.getAllByRole("row")[1]!;
    fireEvent.click(within(row).getByText("note 1"));
    fireEvent.change(within(row).getByRole("textbox"), {
      target: { value: "  mới  " },
    });
    fireEvent.click(
      within(row).getByRole("button", { name: m.table.noteSave }),
    );
    await waitFor(() =>
      expect(apiMock.put).toHaveBeenCalledWith(`${BASE}/1`, {
        note: "mới",
        createIfMissing: false,
      }),
    );
    await waitFor(() =>
      expect(toasts.result.current.at(-1)?.message).toBe(m.toast.noteSaved),
    );
  });

  it("a note save on a bookmark removed elsewhere (PRB-014) says so, closes the editor and reloads", async () => {
    const toasts = renderHook(() => useToasts());
    apiMock.put.mockRejectedValue(new ApiError("PRB-014", 404));
    await renderLoaded();
    const before = listCalls().length;
    const row = screen.getAllByRole("row")[1]!;
    fireEvent.click(within(row).getByText("note 1"));
    fireEvent.click(
      within(row).getByRole("button", { name: m.table.noteSave }),
    );
    await waitFor(() =>
      expect(toasts.result.current.at(-1)).toMatchObject({
        tone: "error",
        message: errors["PRB-014"],
      }),
    );
    await waitFor(() => expect(listCalls().length).toBeGreaterThan(before));
    expect(
      screen.queryByRole("textbox", { name: m.table.noteEditLabel }),
    ).toBeNull();
  });

  it("a too-long note rejected by the server (PRB-001) keeps the editor open", async () => {
    const toasts = renderHook(() => useToasts());
    apiMock.put.mockRejectedValue(new ApiError("PRB-001", 400));
    await renderLoaded();
    const row = screen.getAllByRole("row")[1]!;
    fireEvent.click(within(row).getByText("note 1"));
    fireEvent.click(
      within(row).getByRole("button", { name: m.table.noteSave }),
    );
    await waitFor(() =>
      expect(toasts.result.current.at(-1)?.message).toBe(errors["PRB-001"]),
    );
    expect(within(row).getByRole("textbox")).toBeInTheDocument();
  });
});

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));
