import { beforeEach, describe, expect, it, vi } from "vitest";

const get = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: { get: (...args: unknown[]) => get(...args) },
}));

// Import after the mock so the module talks to the fake `api`.
import {
  realDifficultyBreakdown,
  realSubmissionsByDay,
  realSubmissionsByLanguage,
  realSubmissionsByMonth,
  realSubmissionsSummary,
  realTopProblems,
  realVerdictDistribution,
} from "./real-submissions";

const BASE = "/identity/admin/dashboard";

describe("real admin overview API, judge-backed blocks", () => {
  beforeEach(() => {
    get.mockReset();
  });

  it("reads the total submissions card from #43 and returns it as sent", async () => {
    const summary = {
      value: 12,
      deltaPercent: null,
      deltaDirection: "none",
      sparklineSeries: [0, 0, 0, 0, 0, 0, 12],
    };
    get.mockResolvedValue(summary);

    expect(await realSubmissionsSummary()).toEqual(summary);
    expect(get).toHaveBeenCalledWith(`${BASE}/cross-module`);
  });

  it("maps #67 to the three languages in prototype order with their labels", async () => {
    get.mockResolvedValue({
      pointLabels: ["01/11", "02/11"],
      series: [
        { languageCode: "JAVA", values: [1, 0] },
        { languageCode: "CPP", values: [2, 0] },
        { languageCode: "PYTHON", values: [3, 4] },
      ],
    });

    const result = await realSubmissionsByLanguage();

    expect(get).toHaveBeenCalledWith(`${BASE}/submissions/by-language`);
    expect(result.pointLabels).toEqual(["01/11", "02/11"]);
    expect(result.series.map((s) => [s.labelKey, s.points])).toEqual([
      ["labels.language.PYTHON", [3, 4]],
      ["labels.language.CPP", [2, 0]],
      ["labels.language.JAVA", [1, 0]],
    ]);
  });

  it("drops a language the server sends that the chart does not know", async () => {
    get.mockResolvedValue({
      pointLabels: ["01/11"],
      series: [
        { languageCode: "RUST", values: [9] },
        { languageCode: "JAVA", values: [1] },
      ],
    });

    expect((await realSubmissionsByLanguage()).series).toHaveLength(1);
  });

  it("uses the server percent of #68 as the slice value and names the verdicts", async () => {
    get.mockResolvedValue({
      slices: [
        { verdict: "ACCEPTED", count: 3, percent: 60 },
        { verdict: "WRONG_ANSWER", count: 1, percent: 20 },
        { verdict: "TIME_LIMIT_EXCEEDED", count: 1, percent: 20 },
        { verdict: "RUNTIME_ERROR", count: 0, percent: 0 },
        { verdict: "COMPILE_ERROR", count: 0, percent: 0 },
      ],
    });

    const result = await realVerdictDistribution();

    expect(get).toHaveBeenCalledWith(`${BASE}/submissions/verdicts`);
    expect(result.slices.map((s) => [s.labelKey, s.value])).toEqual([
      ["labels.verdict.ACCEPTED", 60],
      ["labels.verdict.WRONG_ANSWER", 20],
      ["labels.verdict.TIME_LIMIT_EXCEEDED", 20],
      ["labels.verdict.RUNTIME_ERROR", 0],
      ["labels.verdict.COMPILE_ERROR", 0],
    ]);
  });

  it("an unknown verdict code keeps the code as its label and gets no message key", async () => {
    get.mockResolvedValue({
      slices: [{ verdict: "MEMORY_LIMIT_EXCEEDED", count: 1, percent: 100 }],
    });
    const [slice] = (await realVerdictDistribution()).slices;
    expect(slice).toMatchObject({
      label: "MEMORY_LIMIT_EXCEEDED",
      value: 100,
      colorVar: "--color-admin-slate",
    });
    expect(slice).not.toHaveProperty("labelKey");
  });

  it("keys the groups of #69 by level code and labels the two series", async () => {
    get.mockResolvedValue({
      groups: [
        {
          levelId: "a",
          levelCode: "EASY",
          levelDisplayName: "Dễ",
          submissions: 7,
          accepted: 3,
        },
        {
          levelId: "b",
          levelCode: "HARD",
          levelDisplayName: "Khó",
          submissions: 0,
          accepted: 0,
        },
      ],
      legend: ["totalSubmissions", "acceptedSubmissions"],
    });

    const result = await realDifficultyBreakdown();

    expect(get).toHaveBeenCalledWith(`${BASE}/submissions/difficulty`);
    expect(result.levels).toEqual([
      { key: "EASY", total: 7, accepted: 3 },
      { key: "HARD", total: 0, accepted: 0 },
    ]);
    expect(result.legend.map((l) => l.labelKey)).toEqual([
      "labels.legend.totalSubmissions",
      "labels.legend.acceptedSubmissions",
    ]);
  });

  it("turns #70 into weekday labels and dot intensities scaled to the busiest day", async () => {
    get.mockResolvedValue({
      columns: [
        { day: "2026-11-02", count: 0 }, // Monday
        { day: "2026-11-03", count: 5 },
        { day: "2026-11-04", count: 10 },
        { day: "2026-11-05", count: 0 },
        { day: "2026-11-06", count: 0 },
        { day: "2026-11-07", count: 0 },
        { day: "2026-11-08", count: 1 }, // Sunday
      ],
      weekTotal: 16,
      dailyAverage: 2,
    });

    const result = await realSubmissionsByDay();

    expect(get).toHaveBeenCalledWith(`${BASE}/submissions/by-day`);
    expect(result.columns.map((c) => c.label)).toEqual([
      "T2",
      "T3",
      "T4",
      "T5",
      "T6",
      "T7",
      "CN",
    ]);
    expect(result.columns.map((c) => c.intensity)).toEqual([
      0, 5, 10, 0, 0, 0, 1,
    ]);
    expect(result.weekTotal).toBe(16);
    expect(result.dailyAverage).toBe(2);
  });

  it("gives an all-zero week zero intensity instead of dividing by zero", async () => {
    get.mockResolvedValue({
      columns: [{ day: "2026-11-02", count: 0 }],
      weekTotal: 0,
      dailyAverage: 0,
    });

    expect((await realSubmissionsByDay()).columns[0]!.intensity).toBe(0);
  });

  it("labels the months of #71 as T<month> and keeps the total", async () => {
    get.mockResolvedValue({
      points: [
        { month: "2026-06", count: 4 },
        { month: "2026-11", count: 9 },
      ],
      monthTotal: 13,
    });

    const result = await realSubmissionsByMonth();

    expect(get).toHaveBeenCalledWith(`${BASE}/submissions/by-month`);
    expect(result).toEqual({
      points: [
        { label: "T6", value: 4 },
        { label: "T11", value: 9 },
      ],
      monthTotal: 13,
    });
  });

  it("maps the popular problems of #72 to label and count", async () => {
    get.mockResolvedValue({
      items: [{ problemId: "p1", title: "Two Sum", submissionCount: 295 }],
    });

    const result = await realTopProblems();

    expect(get).toHaveBeenCalledWith(`${BASE}/top-problems`);
    expect(result).toEqual({ items: [{ label: "Two Sum", value: 295 }] });
  });

  it("lets a failing block reject so the block shows its own error state", async () => {
    get.mockRejectedValue(new Error("503"));

    await expect(realTopProblems()).rejects.toThrow("503");
  });
});
