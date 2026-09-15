// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { describe, expect, it } from "vitest";
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

  it("difficulty breakdown has exactly 3 groups (Dễ/Trung bình/Khó)", async () => {
    const data = await fakeDifficultyBreakdown();
    expect(difficultyBreakdownSchema.parse(data).groups).toHaveLength(3);
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
