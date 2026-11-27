import { beforeEach, describe, expect, it, vi } from "vitest";

const get = vi.hoisted(() => vi.fn());
vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: { get },
}));

import {
  realListAcceptedSubmissionOptions,
  realListBankQuestionOptions,
} from "./real-entry-options";

describe("entry tab sources", () => {
  beforeEach(() => get.mockReset());

  it("maps accepted submissions with date, code and level, and survives a missing problem", async () => {
    get.mockResolvedValue({
      items: [
        {
          submissionId: "s1",
          problem: { code: "AP001", title: "Two Sum", levelDisplayName: "Dễ" },
          language: "PYTHON",
          submittedAt: "2026-11-22T09:12:00Z",
        },
        {
          submissionId: "s2",
          problem: null,
          language: "CPP",
          submittedAt: "2026-11-20T01:00:00Z",
        },
      ],
    });
    expect(await realListAcceptedSubmissionOptions()).toEqual([
      {
        id: "s1",
        problemTitle: "Two Sum",
        language: "Python",
        submittedAt: "2026-11-22T09:12:00Z",
        problemCode: "AP001",
        levelLabel: "Dễ",
      },
      {
        id: "s2",
        problemTitle: "s2",
        language: "C++",
        submittedAt: "2026-11-20T01:00:00Z",
        problemCode: undefined,
        levelLabel: undefined,
      },
    ]);
    expect(get).toHaveBeenCalledExactlyOnceWith(
      "/judge/me/accepted-submissions",
    );
  });

  it("returns an empty list for a learner without Accepted submissions", async () => {
    get.mockResolvedValue({ items: [] });
    expect(await realListAcceptedSubmissionOptions()).toEqual([]);
  });

  it("maps the first bank page to id and title and keeps the bank size", async () => {
    get.mockResolvedValue({
      items: [{ id: "q1", title: "LRU", topic: {}, level: {} }],
      page: {},
      meta: { shown: 1, total: 24 },
    });
    expect(await realListBankQuestionOptions()).toEqual({
      items: [{ id: "q1", title: "LRU" }],
      total: 24,
    });
    expect(get).toHaveBeenCalledExactlyOnceWith("/interview-bank/questions", {
      page: 1,
    });
  });

  it.each([
    ["bank", realListBankQuestionOptions],
    ["accepted", realListAcceptedSubmissionOptions],
  ])(
    "rejects instead of showing an empty %s list when the server fails",
    async (_name, call) => {
      const error = new Error("503");
      get.mockImplementationOnce(async () => {
        throw error;
      });
      await expect(call()).rejects.toBe(error);
    },
  );
});
