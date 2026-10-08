// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Values ported from 09-layoutBase/Tiến độ của tôi.dc.html `TOPICS`/`DAYS`/`renderVals()` — same
// 1:1-fidelity convention entities/admin-dashboard's mock file documents. Range-dependent recompute
// of acRate/lastSubmittedAt/daily bars is [SoT: Suy luận] (RD mục 3 leaves the exact algorithm open,
// Q1/Q3) — the mock scales the prototype's 30-day numbers down for "7d" and up for "all" so the tabs
// visibly do something, not a real time-window query.
import type {
  Difficulty,
  DifficultyProgress,
  InterviewSummary,
  ProgressOverview,
  ProgressRange,
  SubmissionDailyCount,
  TopicProgress,
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

export function fakeGetInterviewSummary(): Promise<InterviewSummary> {
  return delay(
    {
      completedSessionCount: 23,
      averageScore: 3.8,
      weakestCriterionCode: "PUSHBACK_HANDLING",
      recentSessions: [
        { sessionId: "sess-1", startedAt: "2026-09-18", score: 4.1 },
        { sessionId: "sess-2", startedAt: "2026-09-10", score: 3.6 },
        { sessionId: "sess-3", startedAt: "2026-08-29", score: 3.9 },
      ],
    },
    500,
  );
}
