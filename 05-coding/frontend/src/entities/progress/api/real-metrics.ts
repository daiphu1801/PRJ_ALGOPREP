// Real call of `GET /judge/me/metrics` (03-dd/api/F4_judge-orchestration.md section 12, #12). One reader
// for the three screens that need a piece of it: streak (overview), daily counts (dashboard chart) and
// the per-topic AC rate (topic table). `userId` never travels: the server takes it from the token.
import { api } from "@/shared/api";
import type { ProgressRange } from "../model/types";

export type MyMetrics = {
  streakDays: number;
  lastSubmittedAt: string | null;
  /** `day` is `YYYY-MM-DD` (UTC+7); for range "all" it is the Monday of the ISO week. */
  dailyCounts: { day: string; count: number }[];
  /** Only present when `topicIds` was sent. `acRate` is a 0 to 1 ratio, null with no submission. */
  acRateByTopic?: {
    topicId: string;
    submissionCount: number;
    acceptedCount: number;
    acRate: number | null;
    lastSubmittedAt: string | null;
  }[];
};

/** JDG-V15: at most 50 topic ids per call. */
export const MAX_METRIC_TOPICS = 50;

export function fetchMyMetrics(
  range: ProgressRange,
  topicIds: string[] = [],
): Promise<MyMetrics> {
  return api.get<MyMetrics>("/judge/me/metrics", {
    range,
    ...(topicIds.length > 0 ? { topicIds } : {}),
  });
}
