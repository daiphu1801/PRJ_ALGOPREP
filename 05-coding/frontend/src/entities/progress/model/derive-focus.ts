// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Pure ranking rule, ported verbatim from RD's decision (no AI call):
// 01-rd/screens/users/USR0501_my_progress.md mục 4 Q2 — (1) lowest AC rate, (2) most problems left
// unsolved, (3) longest since last submission, each topic appears at most once, top 3.
import type { FocusSuggestion, TopicProgress } from "./types";

function daysSince(isoDate: string | null, now: Date): number {
  if (!isoDate) return Number.POSITIVE_INFINITY;
  return Math.floor((now.getTime() - new Date(isoDate).getTime()) / 86_400_000);
}

export function deriveFocusSuggestions(
  topics: TopicProgress[],
  now = new Date(),
): FocusSuggestion[] {
  const withUnsolved = topics.filter((t) => t.totalCount > t.solvedCount);
  if (withUnsolved.length === 0) return [];

  const used = new Set<string>();
  const pick: FocusSuggestion[] = [];

  const byLowestAc = [...withUnsolved].sort(
    (a, b) => (a.acRate ?? 0) - (b.acRate ?? 0),
  )[0];
  if (byLowestAc) {
    pick.push({
      topicId: byLowestAc.topicId,
      topicName: byLowestAc.topicName,
      reason: { code: "lowestAcRate", acRate: byLowestAc.acRate ?? 0 },
    });
    used.add(byLowestAc.topicId);
  }

  const byMostUnsolved = [...withUnsolved]
    .filter((t) => !used.has(t.topicId))
    .sort(
      (a, b) => b.totalCount - b.solvedCount - (a.totalCount - a.solvedCount),
    )[0];
  if (byMostUnsolved) {
    pick.push({
      topicId: byMostUnsolved.topicId,
      topicName: byMostUnsolved.topicName,
      reason: {
        code: "mostUnsolved",
        remaining: byMostUnsolved.totalCount - byMostUnsolved.solvedCount,
        acRate: byMostUnsolved.acRate ?? 0,
      },
    });
    used.add(byMostUnsolved.topicId);
  }

  const byLeastRecent = [...withUnsolved]
    .filter((t) => !used.has(t.topicId))
    .sort(
      (a, b) =>
        daysSince(b.lastSubmittedAt, now) - daysSince(a.lastSubmittedAt, now),
    )[0];
  if (byLeastRecent) {
    pick.push({
      topicId: byLeastRecent.topicId,
      topicName: byLeastRecent.topicName,
      reason: {
        code: "leastRecentlySubmitted",
        daysSince: daysSince(byLeastRecent.lastSubmittedAt, now),
      },
    });
  }

  return pick.slice(0, 3);
}
