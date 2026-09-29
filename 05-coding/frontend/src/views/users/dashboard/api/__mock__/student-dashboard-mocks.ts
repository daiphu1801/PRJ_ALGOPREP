// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Values ported from 09-layoutBase/Dashboard AlgoPrep.dc.html `renderVals()` — same 1:1-fidelity
// convention entities/progress and entities/admin-dashboard document in their own mock files.
//
// The prototype fills both the daily series and the 52-week grid from a seeded linear-congruential
// generator rather than hand-written data (dc.html `seriesFor()` / the `hs` loop). That is kept
// here deliberately: 364 hand-written cells would be unreadable, and a seeded generator produces
// the SAME grid on every render, which `Math.random()` would not — a heatmap that reshuffles on
// each navigation looks like a bug to whoever reviews the screen.
import type {
  ActivityCalendar,
  ActivityDay,
  DashboardDailyPoint,
  DashboardRange,
  RecentSolutionReview,
} from "../../model/types";

function delay<T>(value: T, ms = 500): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(value), ms));
}

/** dc.html `seriesFor()` — the same constants, so the shapes match the mockup screenshot. */
function seededSeries(pointCount: number, seed: number, amplitude: number): number[] {
  const out: number[] = [];
  let state = seed;
  for (let i = 0; i < pointCount; i += 1) {
    state = (state * 1103515245 + 12345) % 2147483648;
    out.push(1 + Math.round((state / 2147483648) * amplitude));
  }
  return out;
}

const RANGE_SHAPE: Record<DashboardRange, { points: number; amplitude: number }> = {
  "7d": { points: 7, amplitude: 8 },
  "30d": { points: 15, amplitude: 11 },
  all: { points: 24, amplitude: 14 },
};

// "all" is plotted as a 90-day window: the axis needs a finite span to label, and 90 days is what
// the prototype's widest tab used (dc.html:134-140).
const RANGE_DAYS: Record<DashboardRange, number> = { "7d": 7, "30d": 30, all: 90 };

export function fakeGetDashboardDaily(range: DashboardRange): Promise<DashboardDailyPoint[]> {
  const { points, amplitude } = RANGE_SHAPE[range];
  // dc.html seeds from the label's length; reproduced as an explicit per-range number so the three
  // tabs keep drawing three visibly different curves instead of the same one rescaled.
  const seed = RANGE_DAYS[range] * 7 + 3;
  const counts = seededSeries(points, seed, amplitude);
  const dayStep = RANGE_DAYS[range] / points;
  return delay(
    counts.map((count, index) => ({
      label: `${Math.round((points - index) * dayStep)}d`,
      count,
    })),
  );
}

const WEEK_COUNT = 52;
const MONTH_LABELS = ["T9", "T10", "T11", "T12", "T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8"];

/**
 * Bucket thresholds copied from dc.html: >0.86 → 4, >0.7 → 3, >0.5 → 2, >0.28 → 1, else 0. Keeping
 * the same cut points keeps roughly the same density of dark cells as the mockup.
 */
function levelFor(random: number): ActivityDay["level"] {
  if (random > 0.86) return 4;
  if (random > 0.7) return 3;
  if (random > 0.5) return 2;
  if (random > 0.28) return 1;
  return 0;
}

/**
 * `endDay` is a parameter, not `new Date()` inside the function, so a test can pin the grid. The
 * query below passes a fixed date for the same reason the rest of the mocks use fixed dates: a
 * prototype that renders differently tomorrow is not reviewable.
 */
export function fakeGetActivityCalendar(endDay = "2026-09-27"): Promise<ActivityCalendar> {
  const end = new Date(`${endDay}T00:00:00Z`);
  const totalDays = WEEK_COUNT * 7;
  let state = 987654321;
  let activeDayCount = 0;
  const weeks: ActivityDay[][] = [];

  for (let week = 0; week < WEEK_COUNT; week += 1) {
    const days: ActivityDay[] = [];
    for (let dayOfWeek = 0; dayOfWeek < 7; dayOfWeek += 1) {
      state = (state * 1103515245 + 12345) % 2147483648;
      const level = levelFor(state / 2147483648);
      if (level > 0) activeDayCount += 1;
      const offsetFromEnd = totalDays - 1 - (week * 7 + dayOfWeek);
      const date = new Date(end.getTime() - offsetFromEnd * 86_400_000);
      days.push({
        day: date.toISOString().slice(0, 10),
        // Level 0 means no submission; the rest map onto the tooltip band dc.html shows.
        count: level === 0 ? 0 : level * 2,
        level,
      });
    }
    weeks.push(days);
  }

  return delay({ weeks, monthLabels: MONTH_LABELS, activeDayCount });
}

export function fakeGetRecentSolutionReviews(): Promise<RecentSolutionReview[]> {
  return delay([
    {
      submissionId: "sub-4821",
      problemTitle: "Two Sum",
      reviewedAt: "2026-09-24",
      timeComplexity: "O(n)",
      verdict: "optimal",
    },
    {
      submissionId: "sub-4790",
      problemTitle: "Coin Change",
      reviewedAt: "2026-09-21",
      timeComplexity: "O(n · amount)",
      verdict: "improvable",
    },
    {
      submissionId: "sub-4755",
      problemTitle: "Number of Islands",
      reviewedAt: "2026-09-17",
      timeComplexity: "O(m · n)",
      verdict: "optimal",
    },
  ]);
}
