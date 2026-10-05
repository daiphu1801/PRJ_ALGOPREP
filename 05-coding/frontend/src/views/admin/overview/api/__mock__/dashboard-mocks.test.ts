// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { describe, expect, it } from "vitest";
import type { ProblemLevel } from "@/entities/problem";
import { buildDifficultyGroups } from "../../model/difficulty-groups";
import {
  difficultyBreakdownSchema,
  submissionsByDaySchema,
  submissionsByMonthSchema,
  userRetentionSchema,
  verdictDistributionSchema,
} from "../../model/types";
import {
  fakeDifficultyBreakdown,
  fakeSubmissionsByDay,
  fakeSubmissionsByMonth,
  fakeUserRetention,
  fakeVerdictDistribution,
} from "./dashboard-mocks";

describe("admin-dashboard mocks", () => {
  it("verdict distribution has exactly 5 slices (AC/WA/TLE/RE/CE, DEC-2026-0831-admin-overview-ui-decisions)", async () => {
    const data = await fakeVerdictDistribution();
    expect(verdictDistributionSchema.parse(data).slices).toHaveLength(5);
  });

  it("difficulty breakdown carries one row per seed level key", async () => {
    const data = difficultyBreakdownSchema.parse(await fakeDifficultyBreakdown());
    expect(data.levels.map((l) => l.key)).toEqual(["EASY", "MEDIUM", "HARD"]);
  });

  it("chart groups follow the level list: seed numbers kept, unknown level gets zero bars", async () => {
    const data = difficultyBreakdownSchema.parse(await fakeDifficultyBreakdown());
    const levels: readonly ProblemLevel[] = [
      { key: "EASY", label: "Dễ", tone: "success" },
      { key: "EXTRA", label: "Rất khó", tone: "neutral" },
    ];
    const groups = buildDifficultyGroups(levels, data);
    expect(groups.map((g) => g.label)).toEqual(["Dễ", "Rất khó"]);
    expect(groups[0]!.bars.map((b) => b.value)).toEqual([260, 140]);
    expect(groups[1]!.bars.map((b) => b.value)).toEqual([0, 0]);
  });

  it("submissions-by-day has exactly 7 columns (T2..CN)", async () => {
    const data = await fakeSubmissionsByDay();
    expect(submissionsByDaySchema.parse(data).columns).toHaveLength(7);
  });

  it("submissions-by-month has exactly 6 points (last 6 months)", async () => {
    const data = await fakeSubmissionsByMonth();
    expect(submissionsByMonthSchema.parse(data).points).toHaveLength(6);
  });

  it("user retention has exactly 6 monthly groups", async () => {
    const data = await fakeUserRetention();
    expect(userRetentionSchema.parse(data).groups).toHaveLength(6);
  });
});
