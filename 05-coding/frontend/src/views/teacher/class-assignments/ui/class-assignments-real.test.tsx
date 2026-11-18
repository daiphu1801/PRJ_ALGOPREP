// INS0202 against the REAL api path (F2 #49 to #53): only the HTTP layer is faked. What matters: the
// list is the server page, unknown figures are "-" never 0, a stale removal (PRB-025) reads as its
// own text and reloads, and a mixed assign result is reported per outcome.
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
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: () => unknown, real: () => Promise<T>) => real(),
}));
let params = new URLSearchParams();
vi.mock("next/navigation", () => ({
  useSearchParams: () => params,
  useRouter: () => ({ replace: vi.fn() }),
  usePathname: () => "/instructor/assignments",
}));

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { ClassAssignmentsView } from "./class-assignments-view";

const m = viMessages.classAssignments;
const apiErrors = viMessages.apiErrors as Record<string, string>;
const B = "/problem-bank/instructor";
const page = (n: number) => ({
  number: 1,
  size: 20,
  totalItems: n,
  totalPages: 1,
});

const row = (id: string, over: Record<string, unknown> = {}) => ({
  problemId: id,
  title: `Bài ${id}`,
  topicNames: ["Mảng"],
  levelCode: "EASY",
  levelDisplayName: "Dễ",
  assignedClasses: [{ classId: "c1", className: "Java K21" }],
  submissionCount: 40,
  acRatePercent: 75,
  withdrawn: false,
  ...over,
});

function routes(items = [row("1")], avgAcRatePercent: number | null = null) {
  apiMock.get.mockImplementation(async (path: string) => {
    if (path === "/identity/classes/my")
      return {
        items: [
          {
            id: "c1",
            name: "Java K21",
            description: null,
            schedule: null,
            studentCount: 3,
            createdAt: "2026-11-01T00:00:00Z",
          },
        ],
      };
    if (path === `${B}/class-assignments`)
      return { items, page: page(items.length) };
    if (path === `${B}/class-assignments/summary`)
      return { assignedProblemCount: 7, classCount: 1, avgAcRatePercent };
    if (path === `${B}/classes/c1/assignable-problems`)
      return {
        items: [
          {
            problemId: "n1",
            code: "N-1",
            title: "Bài mới",
            levelCode: "EASY",
            topicNames: ["Mảng"],
          },
        ],
        page: page(1),
      };
    throw new Error(`unexpected GET ${path}`);
  });
}

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

const listCalls = () =>
  apiMock.get.mock.calls.filter((call) => call[0] === `${B}/class-assignments`)
    .length;

async function confirmRemove() {
  fireEvent.click(
    screen.getByRole("button", { name: m.assignmentList.col.btnRemove }),
  );
  fireEvent.click(
    within(await screen.findByRole("dialog")).getByRole("button", {
      name: m.popup.removeAssignmentConfirm,
    }),
  );
}

describe("ClassAssignmentsView with the real API", () => {
  beforeEach(() => {
    resetApiMock();
    params = new URLSearchParams();
    routes();
  });

  it("shows the server page; unknown figures are a dash, the header uses the summary count", async () => {
    routes([row("1", { submissionCount: null, acRatePercent: null })]);
    render(withTestProviders(<ClassAssignmentsView />));
    expect(await screen.findByText("Bài 1")).toBeInTheDocument();
    expect(apiMock.get).toHaveBeenCalledWith(`${B}/class-assignments`, {
      page: 1,
    });
    expect(screen.getAllByText("-").length).toBeGreaterThanOrEqual(2);
    expect(
      await screen.findByText(
        `${m.header.summary.replace("{count}", "7")} · ${m.header.avgAcRate.replace("{value}", "-")}`,
      ),
    ).toBeInTheDocument();
  });

  it("shows the class average from #50 next to the assigned count, and a dash when it is null", async () => {
    routes([row("1")], 62);
    const first = render(withTestProviders(<ClassAssignmentsView />));
    expect(
      await screen.findByText(
        `${m.header.summary.replace("{count}", "7")} · ${m.header.avgAcRate.replace("{value}", "62%")}`,
      ),
    ).toBeInTheDocument();
    first.unmount();
    // A real 0% is a number and is shown as one; only null is unknown.
    routes([row("1")], 0);
    render(withTestProviders(<ClassAssignmentsView />));
    expect(
      await screen.findByText(
        `${m.header.summary.replace("{count}", "7")} · ${m.header.avgAcRate.replace("{value}", "0%")}`,
      ),
    ).toBeInTheDocument();
  });

  it("a class tab in the URL is sent as classId", async () => {
    params = new URLSearchParams("classId=c1");
    render(withTestProviders(<ClassAssignmentsView />));
    await screen.findByText("Bài 1");
    expect(apiMock.get).toHaveBeenCalledWith(`${B}/class-assignments`, {
      classId: "c1",
      page: 1,
    });
  });

  it("a failed list never reads as an empty list", async () => {
    apiMock.get.mockImplementation(async (path: string) => {
      if (path === `${B}/class-assignments`) throw new ApiError("PRB-058", 503);
      if (path === "/identity/classes/my") return { items: [] };
      return { assignedProblemCount: 0, classCount: 0, avgAcRatePercent: null };
    });
    render(withTestProviders(<ClassAssignmentsView />));
    await waitFor(() => expect(listCalls()).toBeGreaterThan(0));
    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(screen.queryByText(m.emptyAssignments)).toBeNull();
    expect(screen.getByText(m.loadFailed)).toBeInTheDocument();
  });

  it("removes with the class and problem ids and toasts the title", async () => {
    apiMock.delete.mockResolvedValue(undefined);
    const toasts = watch();
    render(withTestProviders(<ClassAssignmentsView />));
    await screen.findByText("Bài 1");
    await confirmRemove();
    await waitFor(() =>
      expect(apiMock.delete).toHaveBeenCalledWith(
        `${B}/classes/c1/assignments/1`,
      ),
    );
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual([
        "success",
        m.toast.removed.replace("{title}", "Bài 1"),
      ]),
    );
  });

  it("a stale removal (PRB-025) reads as its own text and the list is loaded again", async () => {
    apiMock.delete.mockRejectedValue(new ApiError("PRB-025", 409));
    const toasts = watch();
    render(withTestProviders(<ClassAssignmentsView />));
    await screen.findByText("Bài 1");
    const before = listCalls();
    await confirmRemove();
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", apiErrors["PRB-025"]]),
    );
    await waitFor(() => expect(listCalls()).toBeGreaterThan(before));
  });

  async function openAssign() {
    render(withTestProviders(<ClassAssignmentsView />));
    await screen.findByText("Bài 1");
    fireEvent.click(
      screen.getByRole("button", { name: m.header.btnAssignFromBank }),
    );
    const dialog = await screen.findByRole("dialog");
    fireEvent.click(await within(dialog).findByLabelText(/Bài mới/));
    fireEvent.click(within(dialog).getByRole("button", { name: "Giao 1 bài" }));
  }

  it("assigns the ticked problem; a mixed result is reported per outcome", async () => {
    apiMock.post.mockResolvedValue({
      succeededIds: [],
      skippedIds: ["x"],
      failedItems: [{ id: "n1", code: "PRB-037", message: "m" }],
    });
    const toasts = watch();
    await openAssign();
    await waitFor(() =>
      expect(apiMock.post).toHaveBeenCalledWith(`${B}/classes/c1/assignments`, {
        problemIds: ["n1"],
      }),
    );
    await waitFor(() => expect(toasts().length).toBe(2));
    expect(toasts()).toEqual([
      ["info", m.toast.assignSkipped.replace("{count}", "1")],
      [
        "warning",
        `${m.toast.assignRefused.replace("{count}", "1")} ${apiErrors["PRB-037"]}`,
      ],
    ]);
  });

  it("a refused class (PRB-005) reads as its own text", async () => {
    apiMock.post.mockRejectedValue(new ApiError("PRB-005", 403));
    const toasts = watch();
    await openAssign();
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", apiErrors["PRB-005"]]),
    );
  });

  it("a clean assign says how many went in", async () => {
    apiMock.post.mockResolvedValue({
      succeededIds: ["n1"],
      skippedIds: [],
      failedItems: [],
    });
    const toasts = watch();
    await openAssign();
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual([
        "success",
        m.toast.assigned
          .replace("{count}", "1")
          .replace("{className}", "Java K21"),
      ]),
    );
  });
});
