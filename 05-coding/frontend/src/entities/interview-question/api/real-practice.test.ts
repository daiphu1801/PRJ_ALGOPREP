import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";

const get = vi.fn();
const post = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: {
    get: (...args: unknown[]) => get(...args),
    post: (...args: unknown[]) => post(...args),
  },
}));

// Import after the mock so the module talks to the fake `api`.
import { realListAttempts, realSubmitPracticeAnswer } from "./real-practice";

const PATH = "/interview-bank/questions/q1/practice-answers";

const completed = {
  id: "a1",
  attemptNo: 2,
  createdAt: "2026-11-11T01:00:00Z",
  answerText: "Chaining dùng list.",
  feedbackStatus: "COMPLETED",
  feedback: {
    achievedPoints: ["Nêu đúng collision"],
    missingPoints: ["Thiếu load factor"],
    improvementSuggestions: ["Thêm ví dụ", "Nêu ngưỡng 0.7"],
    criteria: [
      {
        criterionCode: "Định nghĩa",
        achievementPercent: 90,
        weightPercent: 25.5,
      },
    ],
  },
  feedbackErrorCode: null,
};

beforeEach(() => {
  get.mockReset();
  post.mockReset();
});

describe("realSubmitPracticeAnswer (F6 #6)", () => {
  it("posts the answer text as the only body field and maps a completed attempt", async () => {
    post.mockResolvedValue(completed);
    const attempt = await realSubmitPracticeAnswer("q1", "Chaining dùng list.");
    expect(post).toHaveBeenCalledWith(PATH, {
      answerText: "Chaining dùng list.",
    });
    expect(attempt).toEqual({
      attemptNo: 2,
      createdAt: "2026-11-11T01:00:00Z",
      feedbackStatus: "completed",
      answerText: "Chaining dùng list.",
      feedbackErrorCode: null,
      strengths: ["Nêu đúng collision"],
      gaps: ["Thiếu load factor"],
      nextSteps: "Thêm ví dụ\nNêu ngưỡng 0.7",
      criteria: [
        {
          criterionCode: "Định nghĩa",
          achievementPercent: 90,
          weightPercent: 25.5,
        },
      ],
    });
  });

  it.each(["INB-501", "INB-502"])(
    "keeps %s on a FAILED attempt instead of throwing (the call still answers 201)",
    async (code) => {
      post.mockResolvedValue({
        ...completed,
        feedbackStatus: "FAILED",
        feedback: null,
        feedbackErrorCode: code,
      });
      const attempt = await realSubmitPracticeAnswer("q1", "x");
      expect(attempt.feedbackStatus).toBe("failed");
      expect(attempt.feedbackErrorCode).toBe(code);
      expect(attempt.criteria).toBeUndefined();
    },
  );

  it.each([
    ["INB-V06", 400],
    ["INB-V07", 400],
    ["INB-010", 404],
    ["INB-040", 422],
    ["INB-041", 429],
    ["INB-042", 429],
    ["INB-050", 409],
  ])("rethrows %s", async (code, status) => {
    post.mockRejectedValue(new ApiError(code, status));
    await expect(realSubmitPracticeAnswer("q1", "x")).rejects.toMatchObject({
      code,
    });
  });
});

describe("realListAttempts (F6 #7)", () => {
  it("lists newest first without a cursor on the first call", async () => {
    get.mockResolvedValue({
      items: [completed],
      hasMore: true,
      totalAttempts: 12,
    });
    const page = await realListAttempts("q1");
    expect(get).toHaveBeenCalledWith(PATH, undefined);
    expect(page).toMatchObject({ hasMore: true, totalAttempts: 12 });
    expect(page.items[0]).toMatchObject({
      attemptNo: 2,
      feedbackStatus: "completed",
    });
  });

  it("sends the cursor as `before`", async () => {
    get.mockResolvedValue({ items: [], hasMore: false, totalAttempts: 0 });
    await realListAttempts("q1", 3);
    expect(get).toHaveBeenCalledWith(PATH, { before: 3 });
  });

  it.each([
    ["INB-V08", 400],
    ["INB-010", 404],
  ])("rethrows %s", async (code, status) => {
    get.mockRejectedValue(new ApiError(code, status));
    await expect(realListAttempts("q1")).rejects.toMatchObject({ code });
  });
});
