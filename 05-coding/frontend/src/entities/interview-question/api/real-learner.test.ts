import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";

const get = vi.fn();
const put = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: {
    get: (...args: unknown[]) => get(...args),
    put: (...args: unknown[]) => put(...args),
  },
}));

// Import after the mock so the module talks to the fake `api`.
import {
  realGetQuestion,
  realListDrillCards,
  realListQuestions,
  realRateRecall,
  realSetBookmark,
} from "./real-learner";
import type { LearnerQuestionFilters } from "../model/learner-types";

const NO_FILTERS: LearnerQuestionFilters = {
  q: "",
  status: "all",
  topic: "all",
  level: "all",
  bookmarkedOnly: false,
};

const itemDto = (over: Record<string, unknown> = {}) => ({
  id: "q1",
  topic: { code: "CS_THEORY", displayName: "Lý thuyết CS" },
  level: { code: "EASY", displayName: "Dễ" },
  title: "Process và thread?",
  recallRating: null,
  bookmarked: false,
  ...over,
});

const row = (id: string, recall: string | null, bookmarked: boolean) => ({
  id,
  topic: "CS_THEORY",
  level: "EASY",
  title: "Process và thread?",
  recall,
  bookmarked,
});

beforeEach(() => {
  get.mockReset();
  put.mockReset();
});

describe("realListQuestions (F6 #2)", () => {
  it("sends only the page when no filter is set", async () => {
    get.mockResolvedValue({
      items: [],
      page: { number: 1, size: 20, totalItems: 0, totalPages: 1 },
      meta: { shown: 0, total: 0 },
    });
    await realListQuestions(NO_FILTERS, 1);
    expect(get).toHaveBeenCalledWith("/interview-bank/questions", { page: 1 });
  });

  it("maps the filters to the wire names and the status word to the API one", async () => {
    get.mockResolvedValue({
      items: [],
      page: { number: 2, size: 20, totalItems: 0, totalPages: 2 },
      meta: { shown: 0, total: 0 },
    });
    await realListQuestions(
      {
        q: "hash",
        status: "new",
        topic: "CS_THEORY",
        level: "HARD",
        bookmarkedOnly: true,
      },
      2,
    );
    expect(get).toHaveBeenCalledWith("/interview-bank/questions", {
      q: "hash",
      status: "UNSEEN",
      topic: "CS_THEORY",
      level: "HARD",
      bookmarkedOnly: true,
      page: 2,
    });
  });

  it("maps rows and keeps the server counts from `meta`", async () => {
    get.mockResolvedValue({
      items: [
        itemDto({ recallRating: "VAGUE", bookmarked: true }),
        itemDto({ id: "q2", recallRating: "KNOWN" }),
        itemDto({ id: "q3" }),
      ],
      page: { number: 1, size: 20, totalItems: 45, totalPages: 3 },
      meta: { shown: 45, total: 60 },
    });
    expect(await realListQuestions(NO_FILTERS, 1)).toEqual({
      items: [
        row("q1", "vague", true),
        row("q2", "known", false),
        row("q3", null, false),
      ],
      number: 1,
      totalPages: 3,
      shown: 45,
      total: 60,
    });
  });
});

describe("realListDrillCards (F6 #2 with drill)", () => {
  it("asks for 10 cards with the current filters and keeps the server order", async () => {
    get.mockResolvedValue({
      items: [
        {
          ...itemDto({ id: "b" }),
          suggestedApproach: "- ý một\n- ý hai",
          coreKeywords: ["Hash"],
        },
        { ...itemDto({ id: "a" }), suggestedApproach: null, coreKeywords: [] },
      ],
    });
    const cards = await realListDrillCards({
      ...NO_FILTERS,
      status: "reviewing",
      topic: "DB",
    });
    expect(get).toHaveBeenCalledWith("/interview-bank/questions", {
      status: "REVIEWING",
      topic: "DB",
      drill: true,
      limit: 10,
    });
    expect(cards.map((card) => card.id)).toEqual(["b", "a"]);
    expect(cards[0]).toMatchObject({
      suggestedApproach: ["ý một", "ý hai"],
      coreKeywords: ["Hash"],
    });
    expect(cards[1]).toMatchObject({ suggestedApproach: [], coreKeywords: [] });
  });
});

describe("realGetQuestion (F6 #3)", () => {
  const detailDto = {
    id: "q1",
    topic: {
      code: "BEHAVIORAL",
      displayName: "Hành vi",
      usesStarFramework: true,
    },
    level: { code: "EASY", displayName: "Dễ" },
    title: "Kể về một xung đột",
    contentMarkdown: null,
    suggestedApproach: "- S\n- T",
    sampleAnswerFramework: null,
    coreKeywords: ["STAR"],
    followUpQuestions: ["Kết quả?"],
    bookmarked: true,
    practiceAvailable: false,
    recall: {
      rating: "FORGOTTEN",
      intervalDays: 1,
      nextReviewAt: "2026-11-12T00:00:00Z",
      ratedAt: "2026-11-11T00:00:00Z",
    },
  };

  it("maps the detail, turning null text into empty and the outline into items", async () => {
    get.mockResolvedValue(detailDto);
    expect(await realGetQuestion("q1")).toEqual({
      id: "q1",
      topic: "BEHAVIORAL",
      usesStarFramework: true,
      level: "EASY",
      title: "Kể về một xung đột",
      content: "",
      suggestedApproach: ["S", "T"],
      sampleAnswerFramework: "",
      coreKeywords: ["STAR"],
      followUps: ["Kết quả?"],
      bookmarked: true,
      practiceAvailable: false,
      recall: {
        level: "forgotten",
        intervalDays: 1,
        nextReviewAt: "2026-11-12T00:00:00Z",
        ratedAt: "2026-11-11T00:00:00Z",
      },
    });
    expect(get).toHaveBeenCalledWith("/interview-bank/questions/q1");
  });

  it("maps a missing recall to null", async () => {
    get.mockResolvedValue({ ...detailDto, recall: null });
    expect((await realGetQuestion("q1"))?.recall).toBeNull();
  });

  it("answers null for INB-010 and rethrows anything else", async () => {
    get.mockRejectedValueOnce(new ApiError("INB-010", 404));
    expect(await realGetQuestion("gone")).toBeNull();
    get.mockRejectedValueOnce(new ApiError("UNKNOWN_ERROR", 500));
    await expect(realGetQuestion("q1")).rejects.toMatchObject({
      code: "UNKNOWN_ERROR",
    });
  });
});

describe("realRateRecall (F6 #4)", () => {
  it("puts the API rating and maps the answer", async () => {
    put.mockResolvedValue({
      rating: "VAGUE",
      intervalDays: 3,
      nextReviewAt: "2026-11-14T00:00:00Z",
      ratedAt: "2026-11-11T00:00:00Z",
    });
    expect(await realRateRecall("q1", "vague")).toEqual({
      level: "vague",
      intervalDays: 3,
      nextReviewAt: "2026-11-14T00:00:00Z",
      ratedAt: "2026-11-11T00:00:00Z",
    });
    expect(put).toHaveBeenCalledWith("/interview-bank/questions/q1/recall", {
      rating: "VAGUE",
    });
  });

  it.each([
    ["INB-V04", 400],
    ["INB-010", 404],
  ])("rethrows %s", async (code, status) => {
    put.mockRejectedValue(new ApiError(code, status));
    await expect(realRateRecall("q1", "known")).rejects.toMatchObject({ code });
  });
});

describe("realSetBookmark (F6 #5)", () => {
  it("sends the wanted state, not a toggle, and returns the server state", async () => {
    put.mockResolvedValue({ questionId: "q1", bookmarked: true });
    expect(await realSetBookmark("q1", true)).toBe(true);
    expect(put).toHaveBeenCalledWith("/interview-bank/questions/q1/bookmark", {
      bookmarked: true,
    });
    put.mockResolvedValue({ questionId: "q1", bookmarked: false });
    expect(await realSetBookmark("q1", false)).toBe(false);
    expect(put).toHaveBeenLastCalledWith(
      "/interview-bank/questions/q1/bookmark",
      { bookmarked: false },
    );
  });

  it.each([
    ["INB-V05", 400],
    ["INB-010", 404],
  ])("rethrows %s", async (code, status) => {
    put.mockRejectedValue(new ApiError(code, status));
    await expect(realSetBookmark("q1", true)).rejects.toMatchObject({ code });
  });
});
