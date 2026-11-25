// Giao bài cho lớp against the REAL api path (F2 #51 and #52): only the HTTP layer is faked. What matters: the
// server does the search, level and topic filtering and the paging; a problem the class already has is listed
// dimmed and cannot be ticked; one confirm sends every ticked id and reports each outcome by its own toast.
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../messages/vi.json";
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
const replace = vi.fn();
const push = vi.fn();
vi.mock("next/navigation", () => ({
  useSearchParams: () => params,
  useRouter: () => ({ replace, push }),
  usePathname: () => "/instructor/assignments/new",
}));

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { ClassAssignProblemsView } from "./class-assign-problems-view";

const m = viMessages.classAssignProblems;
const B = "/problem-bank/instructor";
const ASSIGNABLE = `${B}/classes/c1/assignable-problems`;
const page = (n: number, totalPages = 1) => ({
  number: 1,
  size: 20,
  totalItems: n,
  totalPages,
});

const item = (id: string, over: Record<string, unknown> = {}) => ({
  problemId: id,
  code: `P-${id}`,
  title: `Bài ${id}`,
  levelCode: "EASY",
  topicNames: ["Mảng"],
  assigned: false,
  ...over,
});

const classRow = (id: string, name: string) => ({
  id,
  name,
  description: null,
  schedule: null,
  studentCount: 3,
  createdAt: "2026-11-01T00:00:00Z",
});

function routes(items = [item("1"), item("2", { assigned: true })]) {
  apiMock.get.mockImplementation(async (path: string) => {
    if (path === "/identity/classes/my")
      return { items: [classRow("c1", "Java K21"), classRow("c2", "DSA K22")] };
    if (path === ASSIGNABLE) return { items, page: page(items.length) };
    return { items: [] };
  });
}

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () =>
    toasts.result.current.map((entry) => [entry.tone, entry.message]);
}

const assignableCalls = () =>
  apiMock.get.mock.calls.filter((call) => call[0] === ASSIGNABLE);

describe("ClassAssignProblemsView (real api)", () => {
  beforeEach(() => {
    resetApiMock();
    params = new URLSearchParams("classId=c1");
    replace.mockReset();
    push.mockReset();
  });

  it("lists the bank, dims a problem the class already has and keeps it out of the selection", async () => {
    routes();
    render(withTestProviders(<ClassAssignProblemsView />));

    await screen.findByText("Bài 1");
    expect(screen.getByText(m.assigned)).toBeInTheDocument();
    expect(
      screen.getByRole("checkbox", {
        name: m.selectRow.replace("{title}", "Bài 2"),
      }),
    ).toBeDisabled();

    fireEvent.click(screen.getByRole("checkbox", { name: m.selectAll }));
    expect(
      screen.getByRole("checkbox", {
        name: m.selectRow.replace("{title}", "Bài 1"),
      }),
    ).toBeChecked();
    expect(
      screen.getByRole("checkbox", {
        name: m.selectRow.replace("{title}", "Bài 2"),
      }),
    ).not.toBeChecked();
  });

  it("sends the search to the server and goes back to page one", async () => {
    routes();
    render(withTestProviders(<ClassAssignProblemsView />));
    await screen.findByText("Bài 1");

    fireEvent.change(screen.getByPlaceholderText(m.search), {
      target: { value: "word" },
    });
    await waitFor(() =>
      expect(assignableCalls().at(-1)![1]).toMatchObject({
        q: "word",
        page: 1,
      }),
    );
  });

  it("falls back to the first class when the link names none", async () => {
    params = new URLSearchParams();
    routes();
    render(withTestProviders(<ClassAssignProblemsView />));
    await screen.findByText("Bài 1");
    expect(assignableCalls().length).toBeGreaterThan(0);
  });

  it("confirms the whole selection in one call, reports each outcome and returns to the class list", async () => {
    routes([item("1"), item("2"), item("3")]);
    apiMock.post.mockResolvedValue({
      succeededIds: ["1"],
      skippedIds: ["2"],
      failedItems: [{ id: "3", code: "PRB-037", message: "x" }],
    });
    const toasts = watch();
    render(withTestProviders(<ClassAssignProblemsView />));
    await screen.findByText("Bài 1");

    fireEvent.click(screen.getByRole("checkbox", { name: m.selectAll }));
    fireEvent.click(
      await screen.findByRole("button", {
        name: m.confirm.replace("{count}", "3"),
      }),
    );

    await waitFor(() =>
      expect(apiMock.post).toHaveBeenCalledWith(`${B}/classes/c1/assignments`, {
        problemIds: ["1", "2", "3"],
      }),
    );
    await waitFor(() =>
      expect(push).toHaveBeenCalledWith("/instructor/assignments?classId=c1"),
    );
    const tones = toasts().map(([tone]) => tone);
    expect(tones).toEqual(["success", "info", "warning"]);
    expect(
      toasts().every(([, message]) => !String(message).startsWith("toast.")),
    ).toBe(true);
  });

  it("shows an error state instead of a table when the list cannot load", async () => {
    apiMock.get.mockImplementation(async (path: string) => {
      if (path === "/identity/classes/my")
        return { items: [classRow("c1", "Java K21")] };
      if (path === ASSIGNABLE) throw new Error("boom");
      return { items: [] };
    });
    render(withTestProviders(<ClassAssignProblemsView />));
    expect(await screen.findByText(m.loadFailed)).toBeInTheDocument();
  });
  it("opens the problem in a new tab from its title and shows '-' for a problem with no topic", async () => {
    routes([item("1", { topicNames: [] })]);
    render(withTestProviders(<ClassAssignProblemsView />));
    const link = await screen.findByRole("link", { name: "Bài 1" });
    expect(link).toHaveAttribute("href", "/instructor/problems/1");
    expect(link).toHaveAttribute("target", "_blank");
    expect(screen.getByText("-")).toBeInTheDocument();
  });

  it("turns the confirm button off and says why above 50 problems", async () => {
    const many = Array.from({ length: 51 }, (_, i) => item(String(i + 1)));
    routes(many);
    render(withTestProviders(<ClassAssignProblemsView />));
    await screen.findByText("Bài 1");

    fireEvent.click(screen.getByRole("checkbox", { name: m.selectAll }));
    expect(
      await screen.findByText(m.overLimit.replace("{max}", "50")),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: m.confirm.replace("{count}", "51") }),
    ).toBeDisabled();

    fireEvent.click(
      screen.getByRole("checkbox", {
        name: m.selectRow.replace("{title}", "Bài 51"),
      }),
    );
    expect(
      screen.getByRole("button", { name: m.confirm.replace("{count}", "50") }),
    ).toBeEnabled();
  });
});
