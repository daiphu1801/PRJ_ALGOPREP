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
  realRenameTopic,
  realReorderLevels,
  realReorderTopics,
} from "./real-catalog";

beforeEach(() => {
  get.mockReset();
  post.mockReset();
  patch.mockReset();
  put.mockReset();
  del.mockReset();
});

describe("levels (F2 #14 to #18)", () => {
  it("lists in server order, keyed by code, with the id and the admin-only count", async () => {
    get.mockResolvedValue({
      items: [
        {
          id: "l1",
          code: "EASY",
          displayName: "Dễ",
          sortOrder: 1,
          problemCount: 4,
        },
        {
          id: "l2",
          code: "HARD",
          displayName: "Khó",
          sortOrder: 2,
          problemCount: null,
        },
        {
          id: "l3",
          code: "RAT-KHO",
          displayName: "Rất khó",
          sortOrder: 3,
          problemCount: 0,
        },
      ],
    });
    const levels = await realListLevels();
    expect(get).toHaveBeenCalledWith("/problem-bank/levels", {
      includeCounts: true,
    });
    expect(levels).toEqual([
      { id: "l1", key: "EASY", label: "Dễ", tone: "success", problemCount: 4 },
      {
        id: "l2",
        key: "HARD",
        label: "Khó",
        tone: "negative",
        problemCount: null,
      },
      {
        id: "l3",
        key: "RAT-KHO",
        label: "Rất khó",
        tone: "neutral",
        problemCount: 0,
      },
    ]);
  });

  it("sends exactly the documented bodies", async () => {
    await realCreateLevel("Rất khó");
    expect(post).toHaveBeenCalledWith("/problem-bank/levels", {
      displayName: "Rất khó",
    });
    await realRenameLevel("l1", "Nâng cao");
    expect(patch).toHaveBeenCalledWith("/problem-bank/levels/l1", {
      displayName: "Nâng cao",
    });
    await realReorderLevels(["l2", "l1"]);
    expect(put).toHaveBeenCalledWith("/problem-bank/levels/order", {
      orderedIds: ["l2", "l1"],
    });
    await realDeleteLevel("l1");
    expect(del).toHaveBeenCalledWith("/problem-bank/levels/l1");
  });

  it.each([
    ["PRB-022", 409],
    ["PRB-032", 422],
    ["PRB-033", 422],
    ["PRB-049", 422],
    ["PRB-004", 403],
    ["PRB-013", 404],
    ["PRB-001", 400],
  ])(
    "rethrows %s as an ApiError for the screen to translate",
    async (code, status) => {
      del.mockRejectedValue(new ApiError(code, status));
      await expect(realDeleteLevel("l1")).rejects.toMatchObject({
        code,
        status,
      });
    },
  );
});

describe("topics (F2 #19 to #23)", () => {
  it("lists keyed by id, because the filter and the bulk call take the id", async () => {
    get.mockResolvedValue({
      items: [
        {
          id: "t1",
          code: "ARRAY",
          name: "Mảng",
          sortOrder: 1,
          problemCount: 7,
        },
      ],
    });
    expect(await realListTopics()).toEqual([
      { id: "t1", key: "t1", label: "Mảng", problemCount: 7 },
    ]);
    expect(get).toHaveBeenCalledWith("/problem-bank/topics", {
      includeCounts: true,
    });
  });

  it("sends exactly the documented bodies", async () => {
    await realCreateTopic("Quy hoạch động");
    expect(post).toHaveBeenCalledWith("/problem-bank/topics", {
      name: "Quy hoạch động",
    });
    await realRenameTopic("t1", "Mảng một chiều");
    expect(patch).toHaveBeenCalledWith("/problem-bank/topics/t1", {
      name: "Mảng một chiều",
    });
    await realReorderTopics(["t2", "t1"]);
    expect(put).toHaveBeenCalledWith("/problem-bank/topics/order", {
      orderedIds: ["t2", "t1"],
    });
    await realDeleteTopic("t1");
    expect(del).toHaveBeenCalledWith("/problem-bank/topics/t1");
  });

  it.each([
    ["PRB-021", 409],
    ["PRB-031", 422],
    ["PRB-012", 404],
  ])("rethrows %s", async (code, status) => {
    patch.mockRejectedValue(new ApiError(code, status));
    await expect(realRenameTopic("t1", "x")).rejects.toMatchObject({ code });
  });
});
