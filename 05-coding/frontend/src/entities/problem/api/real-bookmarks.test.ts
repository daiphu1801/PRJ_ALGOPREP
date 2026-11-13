import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";

const get = vi.fn();
const put = vi.fn();
const del = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: {
    get: (...args: unknown[]) => get(...args),
    put: (...args: unknown[]) => put(...args),
    delete: (...args: unknown[]) => del(...args),
  },
}));

// Import after the mock so the module talks to the fake `api`.
import {
  realListSaved,
  realRestore,
  realSaveNote,
  realSetBookmarked,
  realUnsave,
} from "./real-bookmarks";
import type { SavedParams } from "../model/saved-types";

const BASE = "/problem-bank/me/bookmarks";

const params: SavedParams = {
  q: "",
  status: "all",
  difficulty: "all",
  page: 1,
};

const dto = {
  problemId: "p1",
  code: "P-1",
  title: "Two Sum",
  levelCode: "EASY",
  levelDisplayName: "Dễ",
  topicNames: ["Mảng"],
  solveState: "SOLVED",
  note: "ghi chú",
  savedAt: "2026-11-01T03:00:00Z",
  withdrawn: false,
};

const page = { number: 1, size: 20, totalItems: 1, totalPages: 1 };

beforeEach(() => {
  get.mockReset();
  put.mockReset();
  del.mockReset();
});

describe("realListSaved (#8)", () => {
  it("sends the default query (newest first, no page size) and maps a row", async () => {
    get.mockResolvedValue({
      items: [dto],
      page,
      meta: { savedTotal: 5, solvedCount: 2, filteredTotal: 1 },
    });
    const result = await realListSaved(params);
    expect(get).toHaveBeenCalledWith(BASE, {
      sort: "savedAt",
      dir: "desc",
      page: 1,
    });
    expect(result.items[0]).toEqual({
      id: "p1",
      code: "P-1",
      title: "Two Sum",
      difficulty: "EASY",
      topics: ["Mảng"],
      solveState: "solved",
      note: "ghi chú",
      savedAt: "2026-11-01T03:00:00Z",
      withdrawn: false,
    });
    expect(result.meta).toEqual({
      savedTotal: 5,
      solvedCount: 2,
      filteredTotal: 1,
    });
  });

  it("turns the filters into the server's parameters", async () => {
    get.mockResolvedValue({
      items: [],
      page,
      meta: { savedTotal: 0, solvedCount: 0, filteredTotal: 0 },
    });
    await realListSaved({
      q: " dp ",
      status: "todo",
      difficulty: "HARD",
      page: 2,
    });
    expect(get).toHaveBeenCalledWith(BASE, {
      q: "dp",
      status: "UNSOLVED",
      difficulty: "HARD",
      sort: "savedAt",
      dir: "desc",
      page: 2,
    });
  });

  it("degraded: an unknown solve state and solved count stay null, a withdrawn row keeps its note", async () => {
    get.mockResolvedValue({
      items: [{ ...dto, solveState: null, withdrawn: true }],
      page,
      meta: { savedTotal: 1, solvedCount: null, filteredTotal: 1 },
    });
    const result = await realListSaved(params);
    expect(result.items[0]).toMatchObject({
      solveState: null,
      withdrawn: true,
      note: "ghi chú",
    });
    expect(result.meta.solvedCount).toBeNull();
  });

  it("rethrows a failure for the screen to translate", async () => {
    get.mockRejectedValue(new ApiError("UNKNOWN_ERROR", 503));
    await expect(realListSaved(params)).rejects.toMatchObject({ status: 503 });
  });
});

describe("writes (#9, #10)", () => {
  it("unsave is a DELETE of that problem", async () => {
    await realUnsave("p1");
    expect(del).toHaveBeenCalledWith(`${BASE}/p1`);
  });

  it("a note edit never creates the bookmark again", async () => {
    await realSaveNote({ problemId: "p1", note: "mới" });
    expect(put).toHaveBeenCalledWith(`${BASE}/p1`, {
      note: "mới",
      createIfMissing: false,
    });
  });

  it("an empty note is sent as such, which clears it", async () => {
    await realSaveNote({ problemId: "p1", note: "" });
    expect(put).toHaveBeenCalledWith(`${BASE}/p1`, {
      note: "",
      createIfMissing: false,
    });
  });

  it("undo puts the bookmark back with its note", async () => {
    await realRestore({ problemId: "p1", note: "ghi chú" });
    expect(put).toHaveBeenCalledWith(`${BASE}/p1`, {
      note: "ghi chú",
      createIfMissing: true,
    });
  });

  it.each([
    ["PRB-014", 404],
    ["PRB-001", 400],
    ["PRB-010", 404],
  ])("rethrows %s from a save", async (code, status) => {
    put.mockRejectedValue(new ApiError(code, status));
    await expect(
      realSaveNote({ problemId: "p1", note: "x" }),
    ).rejects.toMatchObject({ code, status });
  });
});

describe("bookmark state (#11)", () => {
  it("sends the wanted state, not a flip, and returns what the server holds", async () => {
    put.mockResolvedValue({ bookmarked: true });
    expect(
      await realSetBookmarked({ problemId: "p1", bookmarked: true }),
    ).toBe(true);
    expect(put).toHaveBeenCalledWith(`${BASE}/p1/state`, { bookmarked: true });
  });

  it("false is sent as false and the exact body has no other field", async () => {
    put.mockResolvedValue({ bookmarked: false });
    await realSetBookmarked({ problemId: "p1", bookmarked: false });
    expect(put).toHaveBeenCalledWith(`${BASE}/p1/state`, { bookmarked: false });
  });

  it.each([
    ["PRB-001", 400],
    ["PRB-010", 404],
  ])("rethrows %s", async (code, status) => {
    put.mockRejectedValue(new ApiError(code, status));
    await expect(
      realSetBookmarked({ problemId: "p1", bookmarked: true }),
    ).rejects.toMatchObject({ code, status });
  });
});
