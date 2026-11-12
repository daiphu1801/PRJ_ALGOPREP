import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";

const get = vi.fn();
const post = vi.fn();
const patch = vi.fn();
const put = vi.fn();
const del = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: {
    get: (...args: unknown[]) => get(...args),
    post: (...args: unknown[]) => post(...args),
    patch: (...args: unknown[]) => patch(...args),
    put: (...args: unknown[]) => put(...args),
    delete: (...args: unknown[]) => del(...args),
  },
}));

// Import after the mock so the module talks to the fake `api`.
import {
  realCreateLevel,
  realCreateTopic,
  realDeleteLevel,
  realDeleteTopic,
  realListLevels,
  realListTopics,
  realRenameLevel,
  realReorderLevels,
  realReorderTopics,
  realUpdateTopic,
} from "./real-catalog";

beforeEach(() => {
  get.mockReset();
  post.mockReset();
  patch.mockReset();
  put.mockReset();
  del.mockReset();
});

describe("topics (F6 #9, #11 to #14)", () => {
  it("lists in server order, keyed by code, with the id, STAR flag and the admin-only count", async () => {
    get.mockResolvedValue({
      items: [
        {
          id: "t1",
          code: "CS_THEORY",
          displayName: "Lý thuyết CS",
          sortOrder: 1,
          usesStarFramework: false,
          questionCount: 4,
        },
        {
          id: "t2",
          code: "BEHAVIORAL",
          displayName: "Hành vi",
          sortOrder: 2,
          usesStarFramework: true,
          questionCount: null,
        },
      ],
    });
    const topics = await realListTopics();
    expect(get).toHaveBeenCalledWith("/interview-bank/topics", {
      includeCounts: true,
    });
    expect(topics).toEqual([
      {
        id: "t1",
        key: "CS_THEORY",
        label: "Lý thuyết CS",
        usesStarFramework: false,
        questionCount: 4,
      },
      {
        id: "t2",
        key: "BEHAVIORAL",
        label: "Hành vi",
        usesStarFramework: true,
        questionCount: null,
      },
    ]);
  });

  it("creates with the display name only", async () => {
    post.mockResolvedValue({});
    await realCreateTopic("Mạng máy tính");
    expect(post).toHaveBeenCalledWith("/interview-bank/topics", {
      displayName: "Mạng máy tính",
    });
  });

  it("updates the name or the STAR flag, sending only what changed", async () => {
    patch.mockResolvedValue({});
    await realUpdateTopic("t1", { displayName: "Mới" });
    expect(patch).toHaveBeenLastCalledWith("/interview-bank/topics/t1", {
      displayName: "Mới",
    });
    await realUpdateTopic("t1", { usesStarFramework: true });
    expect(patch).toHaveBeenLastCalledWith("/interview-bank/topics/t1", {
      usesStarFramework: true,
    });
  });

  it("reorders with the full id list and deletes by id", async () => {
    put.mockResolvedValue({ items: [] });
    await realReorderTopics(["b", "a"]);
    expect(put).toHaveBeenCalledWith("/interview-bank/topics/order", {
      orderedIds: ["b", "a"],
    });
    del.mockResolvedValue(undefined);
    await realDeleteTopic("t1");
    expect(del).toHaveBeenCalledWith("/interview-bank/topics/t1");
  });

  it.each([
    ["INB-V03", 400],
    ["INB-V13", 400],
    ["INB-V14", 400],
    ["INB-301", 403],
    ["INB-302", 403],
    ["INB-011", 404],
    ["INB-020", 409],
    ["INB-030", 422],
    ["INB-034", 422],
    ["INB-503", 503],
  ])("rethrows %s untouched", async (code, status) => {
    const failure = new ApiError(code, status);
    post.mockRejectedValue(failure);
    patch.mockRejectedValue(failure);
    put.mockRejectedValue(failure);
    del.mockRejectedValue(failure);
    await expect(realCreateTopic("x")).rejects.toMatchObject({ code });
    await expect(
      realUpdateTopic("t", { displayName: "x" }),
    ).rejects.toMatchObject({ code });
    await expect(realReorderTopics(["t"])).rejects.toMatchObject({ code });
    await expect(realDeleteTopic("t")).rejects.toMatchObject({ code });
  });
});

describe("levels (F6 #10, #15 to #18)", () => {
  it("lists in server order with the tone chosen on the client", async () => {
    get.mockResolvedValue({
      items: [
        { id: "l1", code: "EASY", displayName: "Dễ", sortOrder: 1, questionCount: 2 },
        { id: "l2", code: "MEDIUM", displayName: "Trung bình", sortOrder: 2, questionCount: null },
        { id: "l3", code: "HARD", displayName: "Khó", sortOrder: 3, questionCount: 0 },
        { id: "l4", code: "RAT_KHO", displayName: "Rất khó", sortOrder: 4, questionCount: 0 },
      ],
    });
    const levels = await realListLevels();
    expect(get).toHaveBeenCalledWith("/interview-bank/levels", {
      includeCounts: true,
    });
    expect(levels).toEqual([
      { id: "l1", key: "EASY", label: "Dễ", tone: "success", questionCount: 2 },
      { id: "l2", key: "MEDIUM", label: "Trung bình", tone: "warn", questionCount: null },
      { id: "l3", key: "HARD", label: "Khó", tone: "negative", questionCount: 0 },
      { id: "l4", key: "RAT_KHO", label: "Rất khó", tone: "neutral", questionCount: 0 },
    ]);
  });

  it("creates, renames, reorders and deletes on the level paths", async () => {
    post.mockResolvedValue({});
    patch.mockResolvedValue({});
    put.mockResolvedValue({});
    del.mockResolvedValue(undefined);
    await realCreateLevel("Rất khó");
    expect(post).toHaveBeenCalledWith("/interview-bank/levels", {
      displayName: "Rất khó",
    });
    await realRenameLevel("l1", "Cơ bản");
    expect(patch).toHaveBeenCalledWith("/interview-bank/levels/l1", {
      displayName: "Cơ bản",
    });
    await realReorderLevels(["l2", "l1"]);
    expect(put).toHaveBeenCalledWith("/interview-bank/levels/order", {
      orderedIds: ["l2", "l1"],
    });
    await realDeleteLevel("l1");
    expect(del).toHaveBeenCalledWith("/interview-bank/levels/l1");
  });

  it.each([
    ["INB-V03", 400],
    ["INB-V14", 400],
    ["INB-301", 403],
    ["INB-302", 403],
    ["INB-012", 404],
    ["INB-021", 409],
    ["INB-031", 422],
    ["INB-032", 422],
    ["INB-034", 422],
    ["INB-503", 503],
  ])("rethrows %s untouched", async (code, status) => {
    const failure = new ApiError(code, status);
    post.mockRejectedValue(failure);
    patch.mockRejectedValue(failure);
    put.mockRejectedValue(failure);
    del.mockRejectedValue(failure);
    await expect(realCreateLevel("x")).rejects.toMatchObject({ code });
    await expect(realRenameLevel("l", "x")).rejects.toMatchObject({ code });
    await expect(realReorderLevels(["l"])).rejects.toMatchObject({ code });
    await expect(realDeleteLevel("l")).rejects.toMatchObject({ code });
  });
});
