import { beforeEach, describe, expect, it, vi } from "vitest";

const get = vi.hoisted(() => vi.fn());
vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: { get },
}));

import { realGetInterviewTopicProgress } from "./progress-queries";

describe("realGetInterviewTopicProgress", () => {
  beforeEach(() => get.mockReset());

  it("maps every topic, keeping untouched and empty topics", async () => {
    get.mockResolvedValue({
      items: [
        {
          topicId: "t1",
          topicCode: "OS",
          topicName: "Hệ điều hành",
          sortOrder: 1,
          questionsAttempted: 2,
          totalQuestions: 5,
          completionPercent: 40,
          lastPracticedAt: "2026-11-01T03:00:00Z",
        },
        {
          topicId: "t2",
          topicCode: "DB",
          topicName: "CSDL",
          sortOrder: 2,
          questionsAttempted: 0,
          totalQuestions: 0,
          completionPercent: 0,
          lastPracticedAt: null,
        },
      ],
    });
    const rows = await realGetInterviewTopicProgress();
    expect(get).toHaveBeenCalledExactlyOnceWith(
      "/interview-bank/me/topic-progress",
    );
    expect(rows).toEqual([
      {
        topicId: "t1",
        topicName: "Hệ điều hành",
        questionsAttempted: 2,
        totalQuestions: 5,
        lastPracticedAt: "2026-11-01T03:00:00Z",
      },
      {
        topicId: "t2",
        topicName: "CSDL",
        questionsAttempted: 0,
        totalQuestions: 0,
        lastPracticedAt: null,
      },
    ]);
  });

  it("rejects when the server fails instead of inventing rows", async () => {
    const error = new Error("401");
    get.mockImplementationOnce(async () => {
      throw error;
    });
    await expect(realGetInterviewTopicProgress()).rejects.toBe(error);
  });
});
