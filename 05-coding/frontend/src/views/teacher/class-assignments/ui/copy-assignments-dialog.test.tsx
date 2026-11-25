// "Sao chép sang lớp khác" through INS0202 on the REAL api path (F2 #49, #52): only the HTTP layer is faked, by a
// small server that pages #49 by 20 and remembers what #52 assigned. The URL is live, so the class tab really moves.
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { useSyncExternalStore } from "react";
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

const url = vi.hoisted(() => ({
  params: new URLSearchParams(),
  listeners: new Set<() => void>(),
  replace(next: string) {
    url.params = new URLSearchParams(next.split("?")[1] ?? "");
    url.listeners.forEach((listener) => listener());
  },
}));
vi.mock("next/navigation", () => ({
  useSearchParams: () =>
    useSyncExternalStore(
      (listener) => {
        url.listeners.add(listener);
        return () => url.listeners.delete(listener);
      },
      () => url.params,
    ),
  useRouter: () => ({ replace: url.replace }),
  usePathname: () => "/instructor/assignments",
}));

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { ClassAssignmentsView } from "./class-assignments-view";

const m = viMessages.classAssignments;
const copy = m.copy;
const apiErrors = viMessages.apiErrors as Record<string, string>;
const B = "/problem-bank/instructor";

type Problem = { id: string; withdrawn?: boolean };
/** Newest first, as #49 answers. */
let server: Record<string, Problem[]>;
let classIds: string[];
let failList: string | null;

const problems = (n: number, prefix = "p"): Problem[] =>
  Array.from({ length: n }, (_, i) => ({ id: `${prefix}${i + 1}` }));

function routes() {
  apiMock.get.mockImplementation(
    async (path: string, query: { classId?: string; page?: number } = {}) => {
      if (path === "/identity/classes/my")
        return {
          items: classIds.map((id) => ({
            id,
            name: `Lớp ${id}`,
            description: null,
            schedule: null,
            studentCount: 3,
            createdAt: "2026-11-01T00:00:00Z",
          })),
        };
      if (path === `${B}/class-assignments/summary`)
        return { assignedProblemCount: 1, classCount: 2, avgAcRatePercent: 0 };
      if (path === `${B}/class-assignments`) {
        if (failList !== null && query.classId === failList)
          throw new ApiError("PRB-058", 503);
        const all = query.classId ? (server[query.classId] ?? []) : [];
        const number = query.page ?? 1;
        return {
          items: all.slice((number - 1) * 20, number * 20).map((p) => ({
            problemId: p.id,
            title: `Bài ${p.id}`,
            topicNames: ["Mảng"],
            levelCode: "EASY",
            levelDisplayName: "Dễ",
            assignedClasses: [],
            submissionCount: 0,
            acRatePercent: null,
            withdrawn: p.withdrawn ?? false,
          })),
          page: {
            number,
            size: 20,
            totalItems: all.length,
            totalPages: Math.max(1, Math.ceil(all.length / 20)),
          },
        };
      }
      return { items: [] };
    },
  );
  apiMock.post.mockImplementation(serve);
}

async function serve(path: string, body: { problemIds: string[] }) {
  const classId = path.split("/")[4]!;
  const has = new Set(server[classId]!.map((p) => p.id));
  const succeededIds = body.problemIds.filter((id) => !has.has(id));
  for (const id of succeededIds) server[classId]!.unshift({ id });
  return {
    succeededIds,
    skippedIds: body.problemIds.filter((id) => has.has(id)),
    failedItems: [] as { id: string; code: string }[],
  };
}

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

const fill = (text: string, values: Record<string, string | number>) =>
  text.replace(/\{(\w+)\}/g, (_, key: string) => String(values[key]));
const sentSizes = () =>
  apiMock.post.mock.calls.map((call) => call[1].problemIds.length);

const openButton = () => screen.findByRole("button", { name: copy.button });

async function openDialog() {
  render(withTestProviders(<ClassAssignmentsView />));
  fireEvent.click(await openButton());
  return within(await screen.findByRole("dialog"));
}

async function press(
  dialog: ReturnType<typeof within>,
  count: number,
): Promise<void> {
  const button = await dialog.findByRole("button", {
    name: fill(copy.confirm, { count }),
  });
  await waitFor(() => expect(button).toBeEnabled());
  fireEvent.click(button);
}

/** The number printed next to a label of the summary. */
const figure = (dialog: ReturnType<typeof within>, label: string) =>
  dialog.getByText(label).nextElementSibling?.textContent;

describe("copy the problems of a class to another class", () => {
  beforeEach(() => {
    resetApiMock();
    failList = null;
    classIds = ["c1", "c2"];
    server = { c1: problems(3), c2: [] };
    url.params = new URLSearchParams("classId=c1");
    routes();
  });

  it("has no button on the All tab", async () => {
    url.params = new URLSearchParams();
    render(withTestProviders(<ClassAssignmentsView />));
    await screen.findByText(m.emptyAssignments);
    expect(screen.queryByRole("button", { name: copy.button })).toBeNull();
  });

  it("is blocked with its reason when there is no other class", async () => {
    classIds = ["c1"];
    render(withTestProviders(<ClassAssignmentsView />));
    const button = await openButton();
    expect(button).toHaveAttribute("aria-disabled", "true");
    expect(button).toHaveAttribute("title", copy.noOtherClass);
    fireEvent.click(button);
    expect(screen.queryByRole("dialog")).toBeNull();
  });

  it("is blocked when the class has no problem", async () => {
    server.c1 = [];
    render(withTestProviders(<ClassAssignmentsView />));
    await screen.findByText(m.emptyAssignments);
    expect(await openButton()).toHaveAttribute("title", copy.sourceEmpty);
  });

  it("counts what will happen, sends oldest first, reports and moves to the target class", async () => {
    server = {
      c1: [{ id: "a" }, { id: "b" }, { id: "w", withdrawn: true }, { id: "d" }],
      c2: [{ id: "b" }],
    };
    url.params = new URLSearchParams("classId=c1&page=1");
    const toasts = watch();
    const dialog = await openDialog();
    // The only other class is chosen already.
    expect(dialog.getByRole("combobox")).toHaveValue("c2");
    await dialog.findByText(copy.willAssign);
    expect(
      figure(dialog, fill(copy.sourceCount, { className: "Lớp c1" })),
    ).toBe("4");
    expect(figure(dialog, fill(copy.alreadyHas, { className: "Lớp c2" }))).toBe(
      "1",
    );
    expect(figure(dialog, copy.withdrawn)).toBe("1");
    expect(figure(dialog, copy.willAssign)).toBe("2");

    await press(dialog, 2);
    await waitFor(() => expect(screen.queryByRole("dialog")).toBeNull());
    expect(apiMock.post).toHaveBeenCalledExactlyOnceWith(
      `${B}/classes/c2/assignments`,
      { problemIds: ["d", "a"] },
    );
    expect(toasts()).toEqual([
      ["success", fill(copy.done, { count: 2, className: "Lớp c2" })],
    ]);
    expect(url.params.toString()).toBe("classId=c2");
    expect(server.c2!.map((p) => p.id)).toEqual(["a", "d", "b"]);
    expect(server.c1!.map((p) => p.id)).toEqual(["a", "b", "w", "d"]);
  });

  it("does not choose a class or read one while several are possible, and follows the choice", async () => {
    classIds = ["c1", "c2", "c3"];
    server = { c1: problems(3), c2: problems(1), c3: [] };
    const dialog = await openDialog();
    expect(dialog.getByRole("combobox")).toHaveValue("");
    expect(
      dialog.getByRole("button", { name: copy.confirmEmpty }),
    ).toBeDisabled();
    const readClasses = () =>
      apiMock.get.mock.calls
        .filter((call) => call[0] === `${B}/class-assignments`)
        .map((call) => call[1].classId);
    expect(readClasses()).not.toContain("c2");
    expect(readClasses()).not.toContain("c3");

    const choose = (value: string) =>
      fireEvent.change(dialog.getByRole("combobox"), { target: { value } });
    choose("c2");
    await dialog.findByRole("button", {
      name: fill(copy.confirm, { count: 2 }),
    });
    choose("c3");
    // Never the figures of c2 under the name of c3.
    expect(dialog.queryByText(copy.willAssign)).toBeNull();
    await press(dialog, 3);
    await waitFor(() => expect(screen.queryByRole("dialog")).toBeNull());
    expect(apiMock.post.mock.calls[0]![0]).toBe(`${B}/classes/c3/assignments`);
  });

  it("gathers every page of the source and sends 51 problems as 50 and 1", async () => {
    server.c1 = problems(51);
    const dialog = await openDialog();
    await press(dialog, 51);
    await waitFor(() => expect(screen.queryByRole("dialog")).toBeNull());
    expect(sentSizes()).toEqual([50, 1]);
    expect(server.c2!.map((p) => p.id)).toEqual(problems(51).map((p) => p.id));
  });

  it("stops on a failed batch, stays open, and a second press sends only the rest", async () => {
    server.c1 = problems(60);
    apiMock.post
      .mockImplementationOnce(serve)
      .mockRejectedValueOnce(new ApiError("PRB-058", 503));
    const toasts = watch();
    const dialog = await openDialog();
    await press(dialog, 60);
    await waitFor(() => expect(toasts().at(-1)?.[0]).toBe("error"));
    expect(toasts().at(-1)?.[1]).toContain(
      fill(copy.partial, { done: 50, total: 60 }),
    );
    expect(screen.getByRole("dialog")).toBeInTheDocument();

    await press(dialog, 10);
    await waitFor(() => expect(screen.queryByRole("dialog")).toBeNull());
    expect(sentSizes()).toEqual([50, 10, 10]);
    expect(server.c2).toHaveLength(60);
  });

  it("names the refused problems with the reason and still reports the others", async () => {
    apiMock.post.mockResolvedValueOnce({
      succeededIds: ["p3", "p1"],
      skippedIds: [],
      failedItems: [{ id: "p2", code: "PRB-037" }],
    });
    const toasts = watch();
    const dialog = await openDialog();
    await press(dialog, 3);
    await waitFor(() => expect(toasts()).toHaveLength(2));
    expect(toasts()).toContainEqual([
      "warning",
      `${fill(copy.refused, { count: 1, titles: "Bài p2" })} ${apiErrors["PRB-037"]}`,
    ]);
    expect(toasts()).toContainEqual([
      "success",
      fill(copy.done, { count: 2, className: "Lớp c2" }),
    ]);
  });

  it("says nothing is left when the target already has everything", async () => {
    server.c2 = problems(3);
    const dialog = await openDialog();
    await dialog.findByText(fill(copy.nothingLeft, { className: "Lớp c2" }));
    expect(
      dialog.getByRole("button", { name: copy.confirmEmpty }),
    ).toBeDisabled();
  });

  it("a target that cannot be read is an error, never an empty class; retry recovers", async () => {
    failList = "c2";
    const dialog = await openDialog();
    const retry = await dialog.findByRole("button", { name: copy.retry });
    expect(dialog.getByText(m.loadFailed)).toBeInTheDocument();
    expect(dialog.queryByText(copy.willAssign)).toBeNull();
    expect(
      dialog.getByRole("button", { name: copy.confirmEmpty }),
    ).toBeDisabled();

    failList = null;
    fireEvent.click(retry);
    await dialog.findByRole("button", {
      name: fill(copy.confirm, { count: 3 }),
    });
    expect(apiMock.post).not.toHaveBeenCalled();
  });

  it("cannot be closed or sent twice while sending", async () => {
    let release = () => {};
    apiMock.post.mockImplementationOnce(
      async (path: string, body: { problemIds: string[] }) => {
        await new Promise<void>((resolve) => (release = resolve));
        return serve(path, body);
      },
    );
    const dialog = await openDialog();
    await press(dialog, 3);
    const sending = await dialog.findByRole("button", { name: copy.sending });
    expect(sending).toBeDisabled();
    fireEvent.keyDown(document, { key: "Escape" });
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    await act(async () => release());
    await waitFor(() => expect(screen.queryByRole("dialog")).toBeNull());
    expect(apiMock.post).toHaveBeenCalledTimes(1);
  });
});
