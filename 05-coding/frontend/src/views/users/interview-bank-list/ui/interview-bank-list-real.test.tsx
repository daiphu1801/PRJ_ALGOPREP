// USR0401 against the REAL api path (F6 #2 to #5, #9, #10): only the HTTP layer is faked, so the real
// modules, the cache patching, the error-code handling and the real vi texts are what is asserted.
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
import { InterviewBankListView } from "./interview-bank-list-view";

const m = viMessages.interviewBankList;
const errors = viMessages.apiErrors;
const LIST = "/interview-bank/questions";

const topics = [
  { id: "t1", code: "CS_THEORY", displayName: "Lý thuyết CS", sortOrder: 1, usesStarFramework: false, questionCount: null },
];
const levels = [
  { id: "l1", code: "EASY", displayName: "Dễ", sortOrder: 1, questionCount: null },
  { id: "l2", code: "HARD", displayName: "Khó", sortOrder: 2, questionCount: null },
];

const item = (id: string, title: string, over: Record<string, unknown> = {}) => ({
  id,
  topic: { code: "CS_THEORY", displayName: "Lý thuyết CS" },
  level: { code: "EASY", displayName: "Dễ" },
  title,
  recallRating: null,
  bookmarked: false,
  ...over,
});

const detail = (id: string, over: Record<string, unknown> = {}) => ({
  id,
  topic: { code: "CS_THEORY", displayName: "Lý thuyết CS", usesStarFramework: false },
  level: { code: "EASY", displayName: "Dễ" },
  title: `Câu ${id}`,
  contentMarkdown: "Nội dung",
  suggestedApproach: "- Ý thứ nhất\n- Ý thứ hai",
  sampleAnswerFramework: null,
  coreKeywords: ["Hash"],
  followUpQuestions: [],
  bookmarked: false,
  practiceAvailable: true,
  recall: null,
  ...over,
});

const pageOf = (
  items: unknown[],
  number = 1,
  totalPages = 1,
  shown = items.length,
  total = 5,
) => ({
  items,
  page: { number, size: 20, totalItems: shown, totalPages },
  meta: { shown, total },
});

type Params = Record<string, unknown> | undefined;
type Handler = (params: Params) => unknown;

const base = (): Record<string, Handler> => ({
  "/interview-bank/topics": () => ({ items: topics }),
  "/interview-bank/levels": () => ({ items: levels }),
  [LIST]: () => pageOf([item("q1", "Câu một"), item("q2", "Câu hai")]),
  [`${LIST}/q1`]: () => detail("q1"),
  [`${LIST}/q2`]: () => detail("q2"),
});

function serve(over: Record<string, Handler> = {}) {
  const handlers = { ...base(), ...over };
  apiMock.get.mockImplementation(async (path: string, params: Params) => {
    const handler = handlers[path];
    if (!handler) throw new Error(`unexpected GET ${path}`);
    const value = handler(params);
    if (value instanceof Error) throw value;
    return value;
  });
}

const listCalls = () => apiMock.get.mock.calls.filter(([path]) => path === LIST);
const toastsOf = () => renderHook(() => useToasts()).result;
const lastToast = (toasts: { current: readonly { message: string }[] }) =>
  toasts.current.at(-1)?.message;

function openFilter(label: string) {
  fireEvent.click(screen.getByRole("button", { name: new RegExp(`^${label}`) }));
  return screen.getByRole("listbox", { name: label });
}

async function renderLoaded() {
  render(withTestProviders(<InterviewBankListView />));
  await screen.findByRole("button", { name: new RegExp(`^${m.statusFilterLabel}`) });
  await screen.findAllByText("Câu một");
  // The quick view of the first row has its outline once the detail arrived.
  await screen.findByText("Ý thứ nhất");
}

beforeEach(() => {
  resetApiMock();
  serve();
  act(() => toast.clear());
});

describe("InterviewBankListView with the real API", () => {
  it("loads topics, levels and page 1, shows the server counts and the detail of the first row", async () => {
    serve({ [LIST]: () => pageOf([item("q1", "Câu một"), item("q2", "Câu hai")], 1, 1, 2, 9) });
    await renderLoaded();

    expect(listCalls()[0]).toEqual([LIST, { page: 1 }]);
    expect(screen.getByRole("status")).toHaveTextContent("Hiển thị 2 / 9 câu");
    expect(screen.getByRole("heading", { level: 2 })).toHaveTextContent("Câu một");
    expect(apiMock.get).toHaveBeenCalledWith(`${LIST}/q1`);
    // Topic and level labels come from the catalogue, not from the question.
    expect(screen.getAllByText("Lý thuyết CS").length).toBeGreaterThan(0);
    expect(screen.getByRole("link", { name: m.quickView.viewDetail })).toHaveAttribute(
      "href",
      "/interview-bank/q1",
    );
  });

  it("sends the changed filters to the server and shows the new answer", async () => {
    await renderLoaded();
    serve({
      [LIST]: (params) =>
        params?.status === "UNSEEN"
          ? pageOf([item("q9", "Câu chín")], 1, 1, 1, 5)
          : pageOf([item("q1", "Câu một")]),
      [`${LIST}/q9`]: () => detail("q9"),
    });
    fireEvent.click(
      within(openFilter(m.statusFilterLabel)).getByRole("option", { name: m.statusTabs.new }),
    );

    await screen.findAllByText("Câu chín");
    expect(listCalls().at(-1)).toEqual([LIST, { status: "UNSEEN", page: 1 }]);
    expect(screen.getByRole("status")).toHaveTextContent("Hiển thị 1 / 5 câu");
  });

  it("sends the search text once, after the pause, trimmed", async () => {
    await renderLoaded();
    const before = listCalls().length;
    const box = screen.getByPlaceholderText(m.searchPlaceholder);
    fireEvent.change(box, { target: { value: "h" } });
    fireEvent.change(box, { target: { value: "  hash  " } });
    await waitFor(() => expect(listCalls().at(-1)).toEqual([LIST, { q: "hash", page: 1 }]));
    expect(listCalls().length).toBe(before + 1);
  });

  it("loads the next page on demand and merges it without repeating a row", async () => {
    serve({
      [LIST]: (params) =>
        params?.page === 2
          ? pageOf([item("q2", "Câu hai"), item("q3", "Câu ba")], 2, 2, 3, 3)
          : pageOf([item("q1", "Câu một"), item("q2", "Câu hai")], 1, 2, 3, 3),
      [`${LIST}/q3`]: () => detail("q3"),
    });
    await renderLoaded();

    fireEvent.click(screen.getByRole("button", { name: m.loadMore }));
    await screen.findByText("Câu ba");
    expect(listCalls().at(-1)).toEqual([LIST, { page: 2 }]);
    expect(screen.getAllByText("Câu hai")).toHaveLength(1);
    expect(screen.queryByRole("button", { name: m.loadMore })).not.toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("Hiển thị 3 / 3 câu");
  });

  it("keeps the loaded rows and offers a retry when the next page fails", async () => {
    serve({
      [LIST]: (params) =>
        params?.page === 2
          ? new ApiError("UNKNOWN_ERROR", 500)
          : pageOf([item("q1", "Câu một")], 1, 2, 2, 2),
    });
    await renderLoaded();
    fireEvent.click(screen.getByRole("button", { name: m.loadMore }));

    expect(await screen.findByText(m.loadMoreFailed)).toBeInTheDocument();
    expect(screen.getAllByText("Câu một").length).toBeGreaterThan(0);
    expect(screen.getByRole("button", { name: m.loadMoreRetry })).toBeInTheDocument();
  });

  it("rates the selected question with the API rating, patches the row and shows the review date, without reloading the list", async () => {
    await renderLoaded();
    const toasts = toastsOf();
    const listBefore = listCalls().length;
    const nextReviewAt = new Date(Date.now() + 7 * 86_400_000 + 3_600_000).toISOString();
    apiMock.put.mockResolvedValue({
      rating: "KNOWN",
      intervalDays: 7,
      nextReviewAt,
      ratedAt: new Date().toISOString(),
    });

    fireEvent.click(screen.getAllByRole("button", { name: m.recall.known })[0]!);

    await waitFor(() =>
      expect(apiMock.put).toHaveBeenCalledWith(`${LIST}/q1/recall`, { rating: "KNOWN" }),
    );
    expect(await screen.findByText("Sẽ nhắc ôn lại sau 8 ngày")).toBeInTheDocument();
    expect(lastToast(toasts)).toBe(m.toast.rated);
    // The row got the new label in place; the list was not asked again.
    expect(listCalls().length).toBe(listBefore);
  });

  it("says the review is due when the server date is already past", async () => {
    await renderLoaded();
    apiMock.put.mockResolvedValue({
      rating: "VAGUE",
      intervalDays: 3,
      nextReviewAt: new Date(Date.now() - 86_400_000).toISOString(),
      ratedAt: new Date().toISOString(),
    });
    fireEvent.click(screen.getAllByRole("button", { name: m.recall.vague })[0]!);
    expect(await screen.findByText(m.quickView.nextReviewDue)).toBeInTheDocument();
  });

  it.each([
    ["INB-V04", 400, m.toast.rateFailed],
    ["INB-503", 503, m.toast.rateFailed],
  ])("keeps the old rating and shows the rate-failed text on %s", async (code, status, text) => {
    await renderLoaded();
    const toasts = toastsOf();
    apiMock.put.mockRejectedValue(new ApiError(code, status));
    fireEvent.click(screen.getAllByRole("button", { name: m.recall.known })[0]!);
    await waitFor(() => expect(lastToast(toasts)).toBe(text));
    expect(screen.queryByText(/Sẽ nhắc ôn lại/)).not.toBeInTheDocument();
  });

  it("drops the row and says the question is gone when rating answers INB-010", async () => {
    await renderLoaded();
    const toasts = toastsOf();
    apiMock.put.mockRejectedValue(new ApiError("INB-010", 404));
    fireEvent.click(screen.getAllByRole("button", { name: m.recall.known })[0]!);

    await waitFor(() => expect(lastToast(toasts)).toBe(m.toast.unavailable));
    await waitFor(() => expect(screen.queryByText("Câu một")).not.toBeInTheDocument());
    // The first remaining row is selected.
    expect(screen.getByRole("heading", { level: 2 })).toHaveTextContent("Câu hai");
    expect(errors["INB-010"]).toBeTruthy();
  });

  it("flips the bookmark at once, sends the wanted state, and confirms", async () => {
    await renderLoaded();
    const toasts = toastsOf();
    apiMock.put.mockResolvedValue({ questionId: "q1", bookmarked: true });
    const toggle = screen.getAllByRole("button", { name: m.bookmarkLabel })[0]!;
    expect(toggle).toHaveAttribute("aria-pressed", "false");
    fireEvent.click(toggle);

    await waitFor(() =>
      expect(screen.getAllByRole("button", { name: m.bookmarkLabel })[0]).toHaveAttribute("aria-pressed", "true"),
    );
    expect(apiMock.put).toHaveBeenCalledWith(`${LIST}/q1/bookmark`, { bookmarked: true });
    await waitFor(() => expect(lastToast(toasts)).toBe(m.toast.bookmarked));
  });

  it.each([
    ["INB-V05", 400],
    ["INB-503", 503],
  ])("rolls the bookmark back and warns on %s", async (code, status) => {
    await renderLoaded();
    const toasts = toastsOf();
    apiMock.put.mockRejectedValue(new ApiError(code, status));
    fireEvent.click(screen.getAllByRole("button", { name: m.bookmarkLabel })[0]!);

    await waitFor(() => expect(lastToast(toasts)).toBe(m.toast.bookmarkFailed));
    expect(screen.getAllByRole("button", { name: m.bookmarkLabel })[0]).toHaveAttribute("aria-pressed", "false");
  });

  it("drops the row when bookmarking answers INB-010", async () => {
    await renderLoaded();
    const toasts = toastsOf();
    apiMock.put.mockRejectedValue(new ApiError("INB-010", 404));
    fireEvent.click(screen.getAllByRole("button", { name: m.bookmarkLabel })[0]!);
    await waitFor(() => expect(lastToast(toasts)).toBe(m.toast.unavailable));
    await waitFor(() => expect(screen.queryByText("Câu một")).not.toBeInTheDocument());
  });

  it("two quick bookmark clicks on one row send the wanted state each time, in order", async () => {
    await renderLoaded();
    apiMock.put.mockResolvedValue({});
    const toggle = () => screen.getAllByRole("button", { name: m.bookmarkLabel })[0]!;
    fireEvent.click(toggle());
    await waitFor(() => expect(toggle()).toHaveAttribute("aria-pressed", "true"));
    fireEvent.click(toggle());
    await waitFor(() => expect(toggle()).toHaveAttribute("aria-pressed", "false"));
    await waitFor(() =>
      expect(apiMock.put.mock.calls.map(([, body]) => body)).toEqual([
        { bookmarked: true },
        { bookmarked: false },
      ]),
    );
  });

  describe("quick drill", () => {
    const card = (id: string, over: Record<string, unknown> = {}) => ({
      ...item(id, `Thẻ ${id}`),
      suggestedApproach: "- Gợi ý của thẻ",
      coreKeywords: [],
      ...over,
    });

    it("asks the server for 10 cards with the current filters and runs the session without re-sorting", async () => {
      await renderLoaded();
      const toasts = toastsOf();
      serve({
        [LIST]: (params) =>
          params?.drill ? { items: [card("b"), card("a")] } : pageOf([item("q1", "Câu một")]),
      });
      apiMock.put.mockResolvedValue({
        rating: "KNOWN",
        intervalDays: 7,
        nextReviewAt: new Date(Date.now() + 7 * 86_400_000).toISOString(),
        ratedAt: new Date().toISOString(),
      });

      fireEvent.click(screen.getByRole("button", { name: /^Luyện nhanh/ }));
      await screen.findByText("Thẻ b");
      expect(listCalls().at(-1)).toEqual([LIST, { drill: true, limit: 10 }]);
      // The outline stays hidden until the learner asks for it.
      expect(screen.queryByText("Gợi ý của thẻ")).not.toBeInTheDocument();
      fireEvent.click(screen.getByRole("button", { name: m.drill.reveal }));
      expect(screen.getByText("Gợi ý của thẻ")).toBeInTheDocument();

      fireEvent.click(screen.getByRole("button", { name: m.recall.known }));
      expect(await screen.findByText("Thẻ a")).toBeInTheDocument();
      expect(apiMock.put).toHaveBeenCalledWith(`${LIST}/b/recall`, { rating: "KNOWN" });

      fireEvent.click(screen.getByRole("button", { name: m.recall.vague }));
      await waitFor(() =>
        expect(lastToast(toasts)).toBe("Đã ôn xong 2 thẻ."),
      );
      expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent(m.title);
    });

    it("hides the reveal button for a card with no outline", async () => {
      await renderLoaded();
      serve({ [LIST]: () => ({ items: [card("a", { suggestedApproach: null })] }) });
      fireEvent.click(screen.getByRole("button", { name: /^Luyện nhanh/ }));
      await screen.findByText("Thẻ a");
      expect(screen.queryByRole("button", { name: m.drill.reveal })).not.toBeInTheDocument();
    });

    it("stays on the list with a message when the server has no card or fails", async () => {
      await renderLoaded();
      const toasts = toastsOf();
      serve({ [LIST]: () => ({ items: [] }) });
      fireEvent.click(screen.getByRole("button", { name: /^Luyện nhanh/ }));
      await waitFor(() => expect(lastToast(toasts)).toBe(m.toast.drillEmpty));

      serve({ [LIST]: () => new ApiError("UNKNOWN_ERROR", 500) });
      fireEvent.click(screen.getByRole("button", { name: /^Luyện nhanh/ }));
      await waitFor(() => expect(lastToast(toasts)).toBe(m.toast.drillFailed));
      expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent(m.title);
    });

    it("moves on to the next card and warns when rating a card fails", async () => {
      await renderLoaded();
      const toasts = toastsOf();
      serve({ [LIST]: () => ({ items: [card("b"), card("a")] }) });
      apiMock.put.mockRejectedValue(new ApiError("INB-503", 503));
      fireEvent.click(screen.getByRole("button", { name: /^Luyện nhanh/ }));
      await screen.findByText("Thẻ b");
      fireEvent.click(screen.getByRole("button", { name: m.recall.known }));
      expect(await screen.findByText("Thẻ a")).toBeInTheDocument();
      await waitFor(() => expect(lastToast(toasts)).toBe(m.toast.drillRateFailed));
    });
  });

  describe("states", () => {
    it("says the bank is empty when the whole bank has no question", async () => {
      serve({ [LIST]: () => pageOf([], 1, 1, 0, 0) });
      render(withTestProviders(<InterviewBankListView />));
      expect(await screen.findByText(m.emptyBank)).toBeInTheDocument();
      expect(screen.getByRole("button", { name: /^Luyện nhanh/ })).toBeDisabled();
    });

    it("says nothing matches when a filter leaves no row but the bank is not empty", async () => {
      serve({ [LIST]: () => pageOf([], 1, 1, 0, 5) });
      render(withTestProviders(<InterviewBankListView />));
      expect(await screen.findByText(m.emptyFiltered)).toBeInTheDocument();
    });

    it("shows an error with a retry inside the list when the first load fails", async () => {
      let fail = true;
      serve({
        [LIST]: () =>
          fail ? new ApiError("UNKNOWN_ERROR", 500) : pageOf([item("q1", "Câu một")]),
      });
      render(withTestProviders(<InterviewBankListView />));
      const retry = await screen.findByRole("button", { name: viMessages.common.retry });
      // The filter bar is still there.
      expect(screen.getByPlaceholderText(m.searchPlaceholder)).toBeInTheDocument();
      fail = false;
      fireEvent.click(retry);
      expect((await screen.findAllByText("Câu một")).length).toBeGreaterThan(0);
    });

    it("puts the filters back and says so when a reload after a filter change fails", async () => {
      await renderLoaded();
      const toasts = toastsOf();
      serve({
        [LIST]: (params) =>
          params?.level === "HARD"
            ? new ApiError("UNKNOWN_ERROR", 500)
            : pageOf([item("q1", "Câu một"), item("q2", "Câu hai")]),
      });
      fireEvent.click(
        within(openFilter(m.levelFilterLabel)).getByRole("option", { name: "Khó" }),
      );

      await waitFor(() => expect(lastToast(toasts)).toBe(m.toast.loadFailed));
      // The previous list is on screen again with the level filter back on "all".
      expect((await screen.findAllByText("Câu một")).length).toBeGreaterThan(0);
      expect(screen.getByRole("button", { name: new RegExp(`^${m.levelFilterLabel}`) })).toHaveTextContent(m.filterAll);
    });

    it("shows a retry in the quick view when the detail read fails and recovers on retry", async () => {
      let fail = true;
      serve({
        [`${LIST}/q1`]: () => (fail ? new ApiError("UNKNOWN_ERROR", 500) : detail("q1")),
      });
      render(withTestProviders(<InterviewBankListView />));
      expect(await screen.findByText(m.quickView.loadFailed)).toBeInTheDocument();
      // The list on the left is unaffected.
      expect(screen.getAllByText("Câu một").length).toBeGreaterThan(0);
      fail = false;
      fireEvent.click(screen.getByRole("button", { name: m.quickView.retry }));
      expect(await screen.findByText("Ý thứ nhất")).toBeInTheDocument();
    });

    it("drops the row and says so when the detail read answers INB-010", async () => {
      serve({ [`${LIST}/q1`]: () => new ApiError("INB-010", 404) });
      render(withTestProviders(<InterviewBankListView />));
      const toasts = toastsOf();
      await waitFor(() => expect(lastToast(toasts)).toBe(m.toast.unavailable));
      await waitFor(() => expect(screen.queryByText("Câu một")).not.toBeInTheDocument());
      expect(screen.getByRole("heading", { level: 2 })).toHaveTextContent("Câu hai");
    });
  });
});
