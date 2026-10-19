// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Pure ranking rule, ported verbatim from RD's decision (no AI call):
// 01-rd/screens/users/USR0501_my_progress.md mục 4 Q2 — (1) lowest AC rate, (2) most problems left
// unsolved, (3) longest since last submission, each topic appears at most once, top 3.
//
// Owner decision 2026-10-13: the topic the dashboard greeting calls the weakest is the first
// suggestion (while it still has unsolved problems, D9), so the two never name different topics. The caller scores that topic (the 0-100 skill
// score lives in the dashboard view, which this entity must not import) and passes it as `pinned`;
// the three rules above then fill the remaining slots, skipping the pinned topic.
import type { FocusSuggestion, TopicProgress } from "./types";

function daysSince(isoDate: string | null, now: Date): number {
  if (!isoDate) return Number.POSITIVE_INFINITY;
  return Math.floor((now.getTime() - new Date(isoDate).getTime()) / 86_400_000);
}

export function deriveFocusSuggestions(
  topics: TopicProgress[],
  now = new Date(),
  pinned?: { topicId: string; score: number } | null,
): FocusSuggestion[] {
  const withUnsolved = topics.filter((t) => t.totalCount > t.solvedCount);
  // D9 (owner): the weakest topic is pinned only while it still has unsolved problems; otherwise
  // the list can be empty and the card shows its "all solved" message.
  const pinnedTopic = pinned
    ? withUnsolved.find((t) => t.topicId === pinned.topicId)
    : undefined;
  if (withUnsolved.length === 0) return [];

  const used = new Set<string>();
  const pick: FocusSuggestion[] = [];

  if (pinned && pinnedTopic) {
    pick.push({
      topicId: pinnedTopic.topicId,
      topicName: pinnedTopic.topicName,
      reason: { code: "weakestSkill", score: pinned.score },
    });
    used.add(pinnedTopic.topicId);
  }

  const byLowestAc = [...withUnsolved]
    // A topic with no submissions in the range has no AC rate: rank it by other rules, never as 0%.
    .filter((t) => !used.has(t.topicId) && t.acRate !== null)
    .sort((a, b) => (a.acRate ?? 0) - (b.acRate ?? 0))[0];
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
