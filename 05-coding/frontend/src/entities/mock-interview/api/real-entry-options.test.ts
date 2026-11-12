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

  it("maps accepted submissions and survives a missing problem title", async () => {
    get.mockResolvedValue({
      items: [
        {
          submissionId: "s1",
          problem: { title: "Two Sum" },
          language: "PYTHON",
        },
        { submissionId: "s2", problem: null, language: "CPP" },
      ],
    });
    expect(await realListAcceptedSubmissionOptions()).toEqual([
      { id: "s1", problemTitle: "Two Sum", language: "Python" },
      { id: "s2", problemTitle: "s2", language: "C++" },
    ]);
    expect(get).toHaveBeenCalledExactlyOnceWith(
      "/judge/me/accepted-submissions",
    );
  });

  it("returns an empty list for a learner without Accepted submissions", async () => {
    get.mockResolvedValue({ items: [] });
    expect(await realListAcceptedSubmissionOptions()).toEqual([]);
  });

  it("maps the first bank page to id and title", async () => {
    get.mockResolvedValue({
      items: [{ id: "q1", title: "LRU", topic: {}, level: {} }],
      page: {},
    });
    expect(await realListBankQuestionOptions()).toEqual([
      { id: "q1", title: "LRU" },
    ]);
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
