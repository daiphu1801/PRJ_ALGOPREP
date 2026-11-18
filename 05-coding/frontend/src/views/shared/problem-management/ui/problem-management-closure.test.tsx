// Closure of F2 #27/#29/#31: real API boundaries and real translations, only HTTP is replaced.
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
import messages from "../../../../../messages/vi.json";
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
const saveBlob = vi.hoisted(() => vi.fn());
vi.mock("@/shared/lib", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  saveBlob,
}));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { ProblemManagementView } from "./problem-management-view";

const BASE = "/problem-bank/authoring/problems";
const m = messages.problemManagement;
const row = (id: string) => ({
  id,
  code: `P-${id}`,
  title: `Bài ${id}`,
  topicNames: ["Existing"],
  levelId: "level-1",
  levelCode: "EASY",
  levelName: "Easy",
  status: "PUBLISHED",
  submissionCount: 2,
  assignedClassCount: 1,
  acRate: null,
  testcaseCount: 8,
  updatedAt: new Date().toISOString(),
});

function serve() {
  apiMock.get.mockImplementation(async (path: string) => {
    if (path === "/problem-bank/levels")
      return {
        items: [
          {
            id: "level-1",
            code: "EASY",
            displayName: "Easy",
            sortOrder: 1,
            problemCount: 2,
          },
        ],
      };
    if (path === "/problem-bank/topics")
      return {
        items: [
          {
            id: "topic-1",
            code: "ARRAY",
            name: "Arrays",
            sortOrder: 1,
            problemCount: 0,
          },
          {
            id: "topic-2",
            code: "TREE",
            name: "Trees",
            sortOrder: 2,
            problemCount: 0,
          },
        ],
      };
    if (path === BASE)
      return {
        items: [row("1"), row("2")],
        page: { number: 1, size: 8, totalItems: 30, totalPages: 4 },
        totals: { total: 30, published: 30 },
      };
    if (path === `${BASE}/topic-distribution`)
      return { totalProblemsWithTopic: 0, totalTopicsUsed: 0, items: [] };
    if (path === `${BASE}/attention`) return { items: [] };
    throw new Error(`Unexpected GET ${path}`);
  });
}

async function loaded() {
  render(
    withTestProviders(<ProblemManagementView basePath="/admin/problems" />),
  );
  await screen.findByText("Bài 1");
  await waitFor(() =>
    expect(apiMock.get).toHaveBeenCalledWith("/problem-bank/topics", {
      includeCounts: true,
    }),
  );
}

function chooseRows() {
  const rows = within(screen.getByRole("table")).getAllByRole("row");
  fireEvent.click(within(rows[1]!).getByRole("checkbox"));
  fireEvent.click(within(rows[2]!).getByRole("checkbox"));
}

describe("F2 management closure", () => {
  beforeEach(() => {
    resetApiMock();
    saveBlob.mockReset();
    act(() => toast.clear());
    serve();
  });

  it("assigns the picked topic through #31 and reports partial failure instead of a placeholder toast", async () => {
    const toasts = renderHook(() => useToasts());
    apiMock.post.mockResolvedValue({
      succeededIds: ["1"],
      failedItems: [{ id: "2", code: "PRB-003" }],
    });
    await loaded();
    chooseRows();
    fireEvent.click(screen.getByRole("button", { name: m.bulk.assignTopic }));
    const dialog = screen.getByRole("dialog");
    fireEvent.change(within(dialog).getByLabelText(m.bulkTopicLabel), {
      target: { value: "topic-2" },
    });
    expect(apiMock.post).not.toHaveBeenCalled();
    fireEvent.click(
      within(dialog).getByRole("button", { name: m.bulkLevel.apply }),
    );
    await waitFor(() =>
      expect(apiMock.post).toHaveBeenCalledWith(`${BASE}/bulk/topic`, {
        problemIds: ["1", "2"],
        topicId: "topic-2",
      }),
    );
    await waitFor(() =>
      expect(toasts.result.current.at(-1)?.tone).toBe("warning"),
    );
    expect(toasts.result.current.at(-1)?.message).toContain(
      messages.apiErrors["PRB-003"],
    );
    expect(screen.queryByRole("dialog")).toBeNull();
    await waitFor(() =>
      expect(
        apiMock.get.mock.calls.filter(([path]) => path === BASE).length,
      ).toBeGreaterThan(1),
    );
  });

  it("requires confirmation before withdrawing selected problems and sends no status mutation per row", async () => {
    apiMock.post.mockResolvedValue({
      succeededIds: ["1", "2"],
      failedItems: [],
    });
    await loaded();
    chooseRows();
    fireEvent.click(screen.getByRole("button", { name: m.bulk.unpublish }));
    expect(apiMock.post).not.toHaveBeenCalled();
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: m.bulk.unpublish,
      }),
    );
    await waitFor(() =>
      expect(apiMock.post).toHaveBeenCalledExactlyOnceWith(
        `${BASE}/bulk/unpublish`,
        { problemIds: ["1", "2"] },
      ),
    );
    expect(apiMock.put).not.toHaveBeenCalled();
    await waitFor(() => expect(screen.queryByRole("dialog")).toBeNull());
  });

  it("exports all matching rows by filter rather than the visible page or selected ids", async () => {
    const blob = new Blob(["csv"]);
    apiMock.downloadPost.mockResolvedValue({
      blob,
      filename: "all-matching.csv",
    });
    await loaded();
    fireEvent.change(screen.getByLabelText(m.searchLabel), {
      target: { value: "  needle  " },
    });
    await waitFor(() =>
      expect(
        apiMock.get.mock.calls.filter(([path]) => path === BASE).at(-1)?.[1],
      ).toMatchObject({ q: "needle" }),
    );
    fireEvent.click(screen.getByRole("button", { name: m.exportFiltered }));
    await waitFor(() =>
      expect(apiMock.downloadPost).toHaveBeenCalledExactlyOnceWith(
        `${BASE}/export`,
        { filter: { q: "needle" } },
      ),
    );
    expect(saveBlob).toHaveBeenCalledWith(blob, "all-matching.csv");
  });

  it("keeps the topic dialog and selection available after a refused request", async () => {
    const toasts = renderHook(() => useToasts());
    apiMock.post.mockRejectedValue(new ApiError("PRB-002", 403));
    await loaded();
    chooseRows();
    fireEvent.click(screen.getByRole("button", { name: m.bulk.assignTopic }));
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: m.bulkLevel.apply,
      }),
    );
    await waitFor(() =>
      expect(toasts.result.current.at(-1)?.message).toBe(
        messages.apiErrors["PRB-002"],
      ),
    );
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(
      within(screen.getByRole("table")).getAllByRole("checkbox", {
        checked: true,
      }),
    ).toHaveLength(3);
  });
});
