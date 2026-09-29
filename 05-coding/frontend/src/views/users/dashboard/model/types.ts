// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Domain types for USR0601_dashboard (01-rd/screens/users/USR0601_dashboard.md, screen added by
// DEC-2026-0927-student-dashboard-home). Deliberately NARROW: this entity only owns what no
// existing entity already covers. The stat strip, the topic progress bars and the Mock Interview
// block all read `entities/progress`; the suggested-problems table and the resume-draft button
// both read `entities/problem`. Duplicating either here would create a second mock of the same
// numbers that drifts from the first.
//
// Named `student-dashboard`, not `dashboard`, because `entities/admin-dashboard` already exists —
// a bare `dashboard` would read as "the dashboard" and get imported into the wrong area.

/**
 * Structurally identical to `progress`'s `ProgressRange`, spelled out rather than imported because
 * one entity may not import another (FSD, eslint boundaries/element-types). Assignable both ways,
 * so the merged screen drives this block and the topic table from ONE control.
 *
 * It was briefly "7d" | "30d" | "90d" — the dashboard prototype's own tabs
 * (09-layoutBase/Dashboard AlgoPrep.dc.html:134-140) — but after `my_progress` was merged into this
 * screen (DEC-2026-0927-student-area-merge-and-shared-shell) two different range vocabularies on
 * one page would have meant two range controls disagreeing about what "the selected period" means.
 */
export type DashboardRange = "7d" | "30d" | "all";

/** Greeting line + the weakest topic callout under it [SoT: dc.html:101-103]. */
export type DashboardGreeting = {
  firstName: string;
  weakestTopicName: string;
  /** 0-100. */
  weakestTopicAcRate: number;
  /** How many topics the weakest one was picked out of — the prototype says "thấp nhất trong 6 chủ đề". */
  topicCount: number;
};

/** One plotted day in "Bài nộp theo ngày" [SoT: dc.html:142-156]. */
export type DashboardDailyPoint = {
  /** Axis label, e.g. "7d" meaning 7 days ago. */
  label: string;
  count: number;
};

/**
 * One radar axis in "Năng lực theo chủ đề" [SoT: dc.html:160-184]. `score` is 0-100, DERIVED from
 * AC rate and solved ratio (01-rd/screens/users/USR0601_dashboard.md Q2) — not a stored figure and
 * never an AI call. See `derive-skill-radar.ts` for the formula this prototype uses.
 */
export type SkillRadarPoint = {
  topicId: string;
  topicName: string;
  score: number;
};

/**
 * One cell of the 52x7 activity grid [SoT: dc.html:188-213]. `level` 0-4 maps to the 5 legend
 * shades; the raw `count` drives the tooltip.
 */
export type ActivityDay = {
  /** ISO date. */
  day: string;
  count: number;
  level: 0 | 1 | 2 | 3 | 4;
};

export type ActivityCalendar = {
  /** 52 weeks, oldest first; each week is 7 days. */
  weeks: ActivityDay[][];
  /** Month labels along the bottom axis, oldest first. */
  monthLabels: string[];
  activeDayCount: number;
};

/** One row of the "Solution Review" recent block [SoT: dc.html:317-339]. */
export type RecentSolutionReview = {
  submissionId: string;
  problemTitle: string;
  /** ISO date. */
  reviewedAt: string;
  timeComplexity: string;
  /** Matches the prototype's coloured dot: how the report rated the solution overall. */
  verdict: "optimal" | "improvable" | "suboptimal";
};
