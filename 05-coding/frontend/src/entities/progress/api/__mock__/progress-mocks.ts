// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Values ported from 09-layoutBase/Tiến độ của tôi.dc.html `TOPICS`/`DAYS`/`renderVals()` — same
// 1:1-fidelity convention entities/admin-dashboard's mock file documents. Range-dependent recompute
// of acRate/lastSubmittedAt/daily bars is [SoT: Suy luận] (RD mục 3 leaves the exact algorithm open,
// Q1/Q3) — the mock scales the prototype's 30-day numbers down for "7d" and up for "all" so the tabs
// visibly do something, not a real time-window query.
import {
  RUBRIC_CRITERIA,
  type Difficulty,
  type DifficultyProgress,
  type InterviewSummary,
  type ProgressOverview,
  type ProgressRange,
  type RecentInterviewSession,
  type SubmissionDailyCount,
  type TopicProgress,
} from "../../model/types";

function delay<T>(value: T, ms = 500): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(value), ms));
}

const BASE_TOPICS: Omit<TopicProgress, "acRate" | "lastSubmittedAt">[] = [
  {
    topicId: "hash-map",
    topicName: "Hash map",
    solvedCount: 34,
    totalCount: 42,
  },
  {
    topicId: "two-pointers",
    topicName: "Two pointers",
    solvedCount: 28,
    totalCount: 35,
  },
  {
    topicId: "sliding-window",
    topicName: "Sliding window",
    solvedCount: 19,
    totalCount: 30,
  },
  {
    topicId: "graph-bfs-dfs",
    topicName: "Graph / BFS-DFS",
    solvedCount: 22,
    totalCount: 48,
  },
  {
    topicId: "dp",
    topicName: "Dynamic programming",
    solvedCount: 17,
    totalCount: 52,
  },
  {
    topicId: "binary-search",
    topicName: "Binary search",
    solvedCount: 24,
    totalCount: 28,
  },
  {
    topicId: "heap",
    topicName: "Heap / priority queue",
    solvedCount: 12,
    totalCount: 26,
  },
];
const BASE_AC = [71, 66, 58, 44, 31, 74, 49];
const BASE_LAST = [
  "2026-08-20",
  "2026-08-19",
  "2026-08-19",
  "2026-08-15",
  "2026-08-14",
  "2026-08-12",
  "2026-08-08",
];
const RANGE_SCALE: Record<ProgressRange, number> = {
  "7d": 0.6,
  "30d": 1,
  all: 1.3,
};

export function fakeGetProgressOverview(): Promise<ProgressOverview> {
  return delay({
    solvedProblemCount: 182,
    totalPublishedProblemCount: 640,
    totalSubmissions: 418,
    acceptedCount: Math.round(418 * 0.43),
    currentStreakDays: 12,
    avgAttemptsPerSolved: Math.round((418 / 182) * 10) / 10,
  });
}

export function fakeGetTopicProgress(
  range: ProgressRange,
): Promise<TopicProgress[]> {
  const scale = RANGE_SCALE[range];
  return delay(
    BASE_TOPICS.map((topic, i) => ({
      ...topic,
      acRate: Math.max(0, Math.min(100, Math.round(BASE_AC[i]! * scale))),
      lastSubmittedAt: BASE_LAST[i]!,
    })),
    500,
  );
}

const DAILY_COUNTS = [3, 0, 5, 2, 6, 4, 0, 1, 7, 5, 3, 8, 2, 6];

export function fakeGetDailySubmissions(
  range: ProgressRange,
): Promise<SubmissionDailyCount[]> {
  const scale = RANGE_SCALE[range];
  return delay(
    DAILY_COUNTS.map((count, i) => ({
      day: String(8 + i).padStart(2, "0"),
      count: Math.round(count * scale),
    })),
    500,
  );
}

export function fakeGetDifficultyBreakdown(): Promise<DifficultyProgress[]> {
  const rows: [Difficulty, number, number][] = [
    ["EASY", 88, 180],
    ["MEDIUM", 77, 320],
    ["HARD", 17, 140],
  ];
  return delay(
    rows.map(([difficulty, solvedCount, totalCount]) => ({
      difficulty,
      solvedCount,
      totalCount,
    })),
    500,
  );
}

// Interview sessions, newest first, rubric scores on the 0 to 10 scale (owner decision 2026-10-18:
// the former 0-5 numbers doubled). `score` is derived as the mean of the 4 criteria, so the card's
// bars and its total never disagree. Fixed anchor date instead of "now": the range filter stays
// deterministic (no clock, no randomness).
const INTERVIEW_ANCHOR = "2026-09-20";
const RANGE_DAYS: Record<ProgressRange, number> = {
  "7d": 7,
  "30d": 30,
  all: Number.POSITIVE_INFINITY,
};
// Owner decision (D3): the dashboard block shows the 3 most recent sessions.
const RECENT_SESSION_LIMIT = 3;

const round1 = (n: number) => Math.round(n * 10) / 10;
const mean = (values: number[]) =>
  values.reduce((sum, v) => sum + v, 0) / values.length;

// Order matches RUBRIC_CRITERIA: CLARITY, TECHNICAL_ACCURACY, PUSHBACK_HANDLING, COMPLEXITY_AWARENESS.
const SESSION_ROWS: [string, string, number, number, number, number][] = [
  ["sess-1", "2026-09-18", 8.5, 8.5, 7, 8.5],
  ["sess-2", "2026-09-16", 8, 8, 6.5, 8],
  ["sess-3", "2026-09-10", 7.5, 7.5, 6, 7.5],
  ["sess-4", "2026-09-03", 8, 7.5, 6.5, 7],
  ["sess-5", "2026-08-29", 8, 8, 6.5, 7.5],
  ["sess-6", "2026-08-21", 7, 7.5, 6, 7],
  ["sess-7", "2026-08-05", 6.5, 7, 5.5, 6.5],
  ["sess-8", "2026-07-22", 6, 6.5, 5.5, 6],
];

export function fakeGetInterviewSummary(
  range: ProgressRange = "all",
): Promise<InterviewSummary> {
  const anchor = Date.parse(INTERVIEW_ANCHOR);
  const sessions: RecentInterviewSession[] = SESSION_ROWS.filter(
    ([, startedAt]) =>
      (anchor - Date.parse(startedAt)) / 86_400_000 < RANGE_DAYS[range],
  ).map(([sessionId, startedAt, ...scores]) => ({
    sessionId,
    startedAt,
    score: round1(mean(scores)),
    rubricScores: {
      CLARITY: scores[0],
      TECHNICAL_ACCURACY: scores[1],
      PUSHBACK_HANDLING: scores[2],
      COMPLEXITY_AWARENESS: scores[3],
    },
  }));

  const scored = sessions.filter((s) => s.rubricScores !== null);
  // Weakest = lowest mean over the sessions in range; a tie goes to the earlier criterion.
  const weakest = scored.length
    ? RUBRIC_CRITERIA.reduce((low, code) =>
        mean(scored.map((s) => s.rubricScores![code])) <
        mean(scored.map((s) => s.rubricScores![low]))
          ? code
          : low,
      )
    : null;

  return delay(
    {
      completedSessionCount: sessions.length,
      averageScore: scored.length
        ? round1(mean(scored.map((s) => s.score!)))
        : null,
      weakestCriterionCode: weakest,
      recentSessions: sessions.slice(0, RECENT_SESSION_LIMIT),
    },
    500,
  );
}
