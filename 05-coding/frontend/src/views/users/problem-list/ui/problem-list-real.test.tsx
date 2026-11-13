// USR0101 against the REAL api path (F2 #1 to #4, F4 #8): only the HTTP layer is faked, so the real
// modules, the error mapping and the real vi texts are what the assertions read.
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
const push = vi.hoisted(() => vi.fn());
vi.mock("next/navigation", async () =>
  (await import("@/shared/test/mock-url")).navigationMock({ push }),
);

// Import after the mocks.
import { mockUrl } from "@/shared/test/mock-url";
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { ProblemListView } from "./problem-list-view";

const m = viMessages.problemList;
const errors = viMessages.apiErrors;

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
  id,
  code: `P-${id}`,
  title: `Bài ${id}`,
  topicNames: ["Mảng"],
  levelCode: "EASY",
  levelDisplayName: "Dễ",
  acRate: 61.5,
  functionWrapperSupported: true,
  solveState: "SOLVED",
  ...over,
});

type Handler = (params?: Record<string, unknown>) => unknown;

function serve(over: Record<string, Handler> = {}) {
  const handlers: Record<string, Handler> = {
    "/problem-bank/levels": () => ({ items: levels }),
    "/problem-bank/problems": () => ({
      items: [
        item("1"),
        item("2", {
          acRate: null,
          solveState: "ATTEMPTED",
          functionWrapperSupported: false,
        }),
      ],
      page: { number: 1, size: 12, totalItems: 2, totalPages: 1 },
      solveStateAvailable: true,
    }),
    "/problem-bank/topics/progress": () => ({
      items: [
        { topicId: "t1", topicName: "Mảng", sortOrder: 1, solved: 4, total: 9 },
      ],
      solveStateAvailable: true,
    }),
    "/problem-bank/me/class-assignments": () => ({
      groups: [
        {
          classId: "c1",
          className: "Java K21",
          items: [
            {
              problemId: "9",
              title: "Bài lớp",
              solveState: "UNSOLVED",
              assignedAt: "2026-11-01T03:00:00Z",
            },
          ],
        },
      ],
    }),
    "/judge/me/in-progress-problems": () => ({
      items: [
        {
          problemId: "5",
          problem: { title: "Bài dở" },
          language: "JAVA",
          status: "TIME_LIMIT_EXCEEDED",
          passedCount: 3,
          totalCount: 8,
        },
      ],
    }),
    ...over,
  };
  apiMock.get.mockImplementation(
    async (path: string, params?: Record<string, unknown>) => {
      const handler = handlers[path];
      if (!handler) throw new Error(`unexpected GET ${path}`);
      const value = handler(params);
      if (value instanceof Error) throw value;
      return value;
    },
  );
}

const catalogCalls = () =>
  apiMock.get.mock.calls.filter(([path]) => path === "/problem-bank/problems");

async function renderLoaded() {
  render(withTestProviders(<ProblemListView />));
  await screen.findByText("Bài 1");
}

describe("ProblemListView with the real API", () => {
  beforeEach(() => {
    mockUrl.reset("/problems");
    resetApiMock();
    push.mockClear();
    localStorage.clear();
    act(() => toast.clear());
    serve();
  });

  it("draws the server's rows: unknown AC rate as '-', Standard I/O label, chips and both side blocks", async () => {
    await renderLoaded();
    const rows = screen.getAllByRole("row");
    expect(within(rows[1]!).getByText("61.5%")).toBeInTheDocument();
    expect(within(rows[2]!).getByText("-")).toBeInTheDocument();
    expect(
      within(rows[2]!).getByText(m.submissionModel.stdioOnly),
    ).toBeInTheDocument();
    // F5 is not built: no "has a Solution Review" badge is invented.
    expect(screen.queryByText(m.table.hasReview)).toBeNull();
    expect(
      await screen.findByRole("button", { name: "Mảng (4/9)" }),
    ).toBeInTheDocument();
    expect(await screen.findByText("Bài dở")).toBeInTheDocument();
    expect(screen.getByText(/Quá thời gian 3\/8/)).toBeInTheDocument();
    expect(await screen.findByText("Bài lớp")).toBeInTheDocument();
    expect(screen.getByText("Giao ngày 01/11/2026")).toBeInTheDocument();
    expect(catalogCalls()[0]![1]).toEqual({
      sort: "level",
      dir: "asc",
      page: 1,
      pageSize: 12,
    });
    // The in-progress call asks for the screen's cap.
    expect(apiMock.get).toHaveBeenCalledWith("/judge/me/in-progress-problems", {
      limit: 3,
    });
  });

  it("sends the status, level, topic and a debounced search to the server", async () => {
    await renderLoaded();
    fireEvent.click(await screen.findByRole("button", { name: "Mảng (4/9)" }));
    await waitFor(() =>
      expect(catalogCalls().at(-1)![1]).toMatchObject({ topic: "t1", page: 1 }),
    );
    fireEvent.click(
      screen.getByRole("button", {
        name: new RegExp(`^${m.filter.statusLabel}`),
      }),
    );
    fireEvent.click(screen.getByRole("option", { name: m.state.attempted }));
    await waitFor(() =>
      expect(catalogCalls().at(-1)![1]).toMatchObject({ status: "ATTEMPTED" }),
    );
    fireEvent.click(
      screen.getByRole("button", {
        name: new RegExp(`^${m.filter.difficultyLabel}`),
      }),
    );
    fireEvent.click(screen.getByRole("option", { name: "Khó" }));
    await waitFor(() =>
      expect(catalogCalls().at(-1)![1]).toMatchObject({ difficulty: "HARD" }),
    );
    fireEvent.change(screen.getByLabelText(m.filter.searchLabel), {
      target: { value: " coin " },
    });
    await waitFor(() =>
      expect(catalogCalls().at(-1)![1]).toMatchObject({
        q: "coin",
        status: "ATTEMPTED",
        difficulty: "HARD",
        topic: "t1",
      }),
    );
  });

  it("degraded solve state: says so, shows no status, and never invents progress", async () => {
    serve({
      "/problem-bank/problems": () => ({
        items: [item("1", { solveState: null })],
        page: { number: 1, size: 12, totalItems: 1, totalPages: 1 },
        solveStateAvailable: false,
      }),
      "/problem-bank/topics/progress": () => ({
        items: [
          {
            topicId: "t1",
            topicName: "Mảng",
            sortOrder: 1,
            solved: null,
            total: 9,
          },
        ],
        solveStateAvailable: false,
      }),
    });
    await renderLoaded();
    expect(screen.getByText(m.degraded.solveState)).toBeInTheDocument();
    expect(screen.getByText(m.state.unknown)).toBeInTheDocument();
    // The chip keeps the total and drops the "solved/" half instead of showing "0/9".
    expect(
      await screen.findByRole("button", { name: "Mảng (9)" }),
    ).toBeInTheDocument();
  });

  it("a failed side block says so in place and leaves the table and the other block", async () => {
    serve({
      "/judge/me/in-progress-problems": () =>
        new ApiError("UNKNOWN_ERROR", 503),
    });
    await renderLoaded();
    expect(await screen.findByText(m.blockFailed)).toBeInTheDocument();
    expect(await screen.findByText("Bài lớp")).toBeInTheDocument();
    expect(screen.getAllByRole("row").length).toBeGreaterThan(1);
  });

  it("class assignments: an unknown solve state draws no badge", async () => {
    serve({
      "/problem-bank/me/class-assignments": () => ({
        groups: [
          {
            classId: "c1",
            className: "Java K21",
            items: [
              {
                problemId: "9",
                title: "Bài lớp",
                solveState: null,
                assignedAt: "2026-11-01T03:00:00Z",
              },
            ],
          },
        ],
      }),
    });
    await renderLoaded();
    const link = (await screen.findByText("Bài lớp")).closest("a")!;
    expect(within(link).queryByText(m.state.todo)).toBeNull();
    expect(within(link).queryByText(m.state.solved)).toBeNull();
  });

  it("the table failing shows the retry block, not an empty table", async () => {
    serve({
      "/problem-bank/problems": () => new ApiError("UNKNOWN_ERROR", 503),
    });
    render(withTestProviders(<ProblemListView />));
    expect(
      await screen.findByText(viMessages.common.error),
    ).toBeInTheDocument();
    expect(screen.queryByRole("table")).toBeNull();
  });

  it("random: asks the server with the current filters and opens the answer", async () => {
    serve({ "/problem-bank/problems/random": () => ({ problemId: "77" }) });
    await renderLoaded();
    fireEvent.click(
      screen.getByRole("button", { name: new RegExp(m.summary.btnRandom) }),
    );
    await waitFor(() => expect(push).toHaveBeenCalledWith("/problems/77"));
    expect(apiMock.get).toHaveBeenCalledWith(
      "/problem-bank/problems/random",
      {},
    );
  });

  it("random: an empty set warns instead of navigating", async () => {
    const toasts = renderHook(() => useToasts());
    serve({ "/problem-bank/problems/random": () => ({ problemId: null }) });
    await renderLoaded();
    fireEvent.click(
      screen.getByRole("button", { name: new RegExp(m.summary.btnRandom) }),
    );
    await waitFor(() => expect(toasts.result.current).toHaveLength(1));
    expect(push).not.toHaveBeenCalled();
    expect(toasts.result.current[0]).toMatchObject({
      tone: "warning",
      message: m.toast.randomEmpty,
    });
  });

  it("random: a failure shows the translated reason", async () => {
    const toasts = renderHook(() => useToasts());
    serve({
      "/problem-bank/problems/random": () => new ApiError("PRB-057", 503),
    });
    await renderLoaded();
    fireEvent.click(
      screen.getByRole("button", { name: new RegExp(m.summary.btnRandom) }),
    );
    await waitFor(() => expect(toasts.result.current).toHaveLength(1));
    expect(toasts.result.current[0]!.message).toBe(errors["PRB-057"]);
  });
});

describe("ProblemListView URL state and block visibility", () => {
  beforeEach(() => {
    resetApiMock();
    push.mockClear();
    localStorage.clear();
    act(() => toast.clear());
    serve();
  });

  it("writes every filter, the sort and the page to the URL, and keeps defaults out of it", async () => {
    mockUrl.reset("/problems");
    await renderLoaded();
    expect(mockUrl.href).toBe("/problems");

    fireEvent.click(await screen.findByRole("button", { name: "Mảng (4/9)" }));
    fireEvent.click(
      screen.getByRole("button", {
        name: new RegExp(`^${m.filter.statusLabel}`),
      }),
    );
    fireEvent.click(screen.getByRole("option", { name: m.state.attempted }));
    fireEvent.click(
      screen.getByRole("button", {
        name: new RegExp(`^${m.filter.difficultyLabel}`),
      }),
    );
    fireEvent.click(screen.getByRole("option", { name: "Khó" }));
    fireEvent.change(screen.getByLabelText(m.filter.searchLabel), {
      target: { value: " coin " },
    });
    fireEvent.click(screen.getByRole("button", { name: m.table.colAcRate }));

    const search = () => Object.fromEntries(mockUrl.search);
    await waitFor(() =>
      expect(search()).toMatchObject({
        topic: "t1",
        status: "attempted",
        q: "coin",
        sort: "acRate",
      }),
    );
    expect(search().level).toBeTruthy();
    // Ascending is the default direction, and page 1 the default page.
    expect(search()).not.toHaveProperty("dir");
    expect(search()).not.toHaveProperty("page");

    // A second click on the same header flips the direction.
    fireEvent.click(screen.getByRole("button", { name: m.table.colAcRate }));
    await waitFor(() => expect(search().dir).toBe("desc"));
  });

  it("restores everything from the URL on load and sends it with the first request", async () => {
    mockUrl.reset(
      "/problems?q=coin&status=attempted&level=HARD&topic=t1&sort=acRate&dir=desc&page=2",
    );
    serve({
      "/problem-bank/problems": () => ({
        items: [item("1")],
        page: { number: 2, size: 12, totalItems: 20, totalPages: 2 },
        solveStateAvailable: true,
      }),
    });
    await renderLoaded();
    expect(screen.getByLabelText(m.filter.searchLabel)).toHaveValue("coin");
    expect(catalogCalls().at(-1)![1]).toMatchObject({
      q: "coin",
      status: "ATTEMPTED",
      topic: "t1",
      sort: "acRate",
      dir: "desc",
      page: 2,
    });
  });

  it("an unknown status, sort or page in the URL falls back to the defaults", async () => {
    mockUrl.reset("/problems?status=bogus&sort=bogus&dir=up&page=-4");
    await renderLoaded();
    const sent = catalogCalls().at(-1)![1] as Record<string, unknown>;
    // The wire has no status filter for "all", and the difficulty sort goes out as the server calls it.
    expect(sent).toMatchObject({ sort: "level", dir: "asc", page: 1 });
    expect(sent).not.toHaveProperty("status");
  });

  it("gives each solve link the problem title as its accessible name", async () => {
    mockUrl.reset("/problems");
    await renderLoaded();
    for (const title of ["Bài 1", "Bài 2"]) {
      expect(
        screen.getByRole("link", {
          name: m.table.btnSolveFor.replace("{title}", title),
        }),
      ).toHaveAttribute("href", `/problems/${title.slice(-1)}`);
    }
  });

  it("leaves out the in-progress and class blocks when they are empty, but keeps the table", async () => {
    mockUrl.reset("/problems");
    serve({
      "/problem-bank/me/class-assignments": () => ({ groups: [] }),
      "/judge/me/in-progress-problems": () => ({ items: [] }),
    });
    await renderLoaded();
    await waitFor(() =>
      expect(
        screen.queryByRole("heading", { name: m.inProgress.title }),
      ).not.toBeInTheDocument(),
    );
    expect(
      screen.queryByRole("heading", { name: m.assignment.title }),
    ).not.toBeInTheDocument();
    expect(screen.getByText("Bài 1")).toBeInTheDocument();
  });

  it("keeps a block that failed visible, and the other empty one hidden", async () => {
    mockUrl.reset("/problems");
    serve({
      "/problem-bank/me/class-assignments": () => ({ groups: [] }),
      "/judge/me/in-progress-problems": () => new ApiError("PRB-057", 503),
    });
    await renderLoaded();
    expect(
      await screen.findByRole("heading", { name: m.inProgress.title }),
    ).toBeInTheDocument();
    expect(await screen.findByText(m.blockFailed)).toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: m.assignment.title }),
    ).not.toBeInTheDocument();
  });
});
