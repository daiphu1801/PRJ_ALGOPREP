// Real calls behind `entities/progress`:
//   - overview: F1 #47 `GET /identity/me/progress/summary` (03-dd/api/F1_identity.md section 0.8.4), plus
//     `streakDays` from F4 `/judge/me/metrics` and `totalPublished` from F2 #13B; the two extras degrade to
//     null ("-" on screen) when their module is down, the F1 figures do not.
//   - difficulty: F2 #13B `GET /problem-bank/me/levels/progress` (03-dd/api/F2_problem-bank.md 14A).
//   - topics: F2 #3 `GET /problem-bank/topics/progress` joined with F4 `/judge/me/metrics?topicIds=`.
// Nothing is invented: a figure the server could not give is null, never 0.
import { ApiError, api } from "@/shared/api";
import type {
  DifficultyProgress,
  ProgressOverview,
  ProgressRange,
  TopicProgress,
} from "../model/types";
import { fetchMyMetrics, MAX_METRIC_TOPICS } from "./real-metrics";

type ProgressSummaryDto = {
  solvedProblemCount: number;
  totalSubmissions: number;
  acceptedCount: number;
  acceptanceRatePercent: number | null;
  firstTryAcceptedCount: number;
  topLanguage: "JAVA" | "CPP" | "PYTHON" | null;
};

type LevelProgressDto = {
  totalPublished: number;
  solved: number;
  byLevel: {
    levelCode: string;
    levelDisplayName: string;
    sortOrder: number;
    total: number;
    solved: number;
  }[];
};

const getLevelProgress = () =>
  api.get<LevelProgressDto>("/problem-bank/me/levels/progress");

export async function realGetProgressOverview(): Promise<ProgressOverview> {
  const [summary, metrics, levels] = await Promise.allSettled([
    api.get<ProgressSummaryDto>("/identity/me/progress/summary"),
    fetchMyMetrics("all"),
    getLevelProgress(),
  ]);
  // The F1 figures are the card's core: their failure is the card's failure.
  if (summary.status === "rejected") throw summary.reason;
  const dto = summary.value;
  // `firstTryAcceptedCount` and `topLanguage` are not read by any screen yet; dropped here.
  return {
    solvedProblemCount: dto.solvedProblemCount,
    totalPublishedProblemCount:
      levels.status === "fulfilled" ? levels.value.totalPublished : null,
    totalSubmissions: dto.totalSubmissions,
    acceptedCount: dto.acceptedCount,
    acceptanceRatePercent: dto.acceptanceRatePercent,
    currentStreakDays:
      metrics.status === "fulfilled" ? metrics.value.streakDays : null,
    // No source: needs per-problem attempt counts that no endpoint exposes (PROTOTYPE_DEBT, F1-3).
    avgAttemptsPerSolved: null,
  };
}

/** #13B: one row per level of the admin's list; `difficulty` is the level `code`, equal to `level.key`. */
export async function realGetDifficultyBreakdown(): Promise<
  DifficultyProgress[]
> {
  const dto = await getLevelProgress();
  return dto.byLevel.map((level) => ({
    difficulty: level.levelCode,
    solvedCount: level.solved,
    totalCount: level.total,
  }));
}

// Small repeat of `entities/problem` `realTopicProgress` (#3): FSD forbids one entity importing another
// and the screen needs the ids and names, not the problem-list shape.
type TopicsDto = {
  items: {
    topicId: string;
    topicName: string;
    solved: number | null;
    total: number;
  }[];
  solveStateAvailable: boolean;
};

/**
 * F2 #3 gives the topics with solved/total (cumulative); F4 adds `acRate` and `lastSubmittedAt` inside the
 * range. F2 down, or its solve state unavailable: throw, so the block shows its error state and never a
 * made-up 0. F4 down: keep solved/total and leave the two range figures null.
 */
export async function realGetTopicProgress(
  range: ProgressRange,
): Promise<TopicProgress[]> {
  const dto = await api.get<TopicsDto>("/problem-bank/topics/progress");
  if (!dto.solveStateAvailable) {
    throw new ApiError("PRB-057", 503, "solve state unavailable");
  }

  const byTopic = new Map<
    string,
    { acRate: number | null; lastSubmittedAt: string | null }
  >();
  try {
    const ids = dto.items.map((item) => item.topicId);
    // ponytail: one request per 50 topics, sequential; a catalogue has a few dozen topics at most.
    for (let from = 0; from < ids.length; from += MAX_METRIC_TOPICS) {
      const metrics = await fetchMyMetrics(
        range,
        ids.slice(from, from + MAX_METRIC_TOPICS),
      );
      for (const row of metrics.acRateByTopic ?? []) {
        byTopic.set(row.topicId, {
          acRate: row.acRate === null ? null : Math.round(row.acRate * 100),
          lastSubmittedAt: row.lastSubmittedAt,
        });
      }
    }
  } catch {
    byTopic.clear();
  }

  return dto.items.map((item) => ({
    topicId: item.topicId,
    topicName: item.topicName,
    solvedCount: item.solved ?? 0,
    totalCount: item.total,
    acRate: byTopic.get(item.topicId)?.acRate ?? null,
    lastSubmittedAt: byTopic.get(item.topicId)?.lastSubmittedAt ?? null,
  }));
}
