// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Pure derivation, no AI call and no endpoint of its own — the rule RD settled at
// 01-rd/screens/users/USR0601_dashboard.md mục 4 Q2: the 0-100 skill score comes from the AC rate
// (F1-07) adjusted by how much of the topic has actually been solved (F1-06). Same shape as
// `entities/progress/model/derive-focus.ts`, which settled the sibling "Nên ưu tiên" rule the same
// way.
//
// Lives under views/dashboard, NOT entities/student-dashboard, because it reads `TopicProgress`
// from `entities/progress` and FSD forbids one entity importing another (eslint
// boundaries/element-types, 01-rd/system/SYS0102_frontend_architecture.md mục 2). Combining two
// entities is exactly what the layer above them is for, and three blocks on this screen share it —
// greeting, radar and suggested problems — so it is screen-level shared logic, not block-level.
//
// The weighting is locked by the owner (2026-10-18, DEC-2026-1018-usr-open-questions-phase2-5): 70% AC
// rate plus 30% solved ratio of the topic, with NO difficulty weight (difficulty is an admin-managed
// catalogue with no logic attached). Why those two terms: AC rate alone rates a student who solved 2
// easy problems at 100, and coverage alone ignores whether they got them right.
import type { TopicProgress } from "@/entities/progress";
import type { SkillRadarPoint } from "../api";

const AC_WEIGHT = 0.7;
const COVERAGE_WEIGHT = 0.3;

/** Number of axes the radar draws. dc.html:170-173 plots 6; more than that and the labels collide. */
export const RADAR_AXIS_COUNT = 6;

/** Every topic scored, in the caller's order. The greeting and the ranking read this, not the radar. */
export function scoreTopics(topics: TopicProgress[]): SkillRadarPoint[] {
  return topics.map((topic) => {
    const coverage =
      topic.totalCount === 0 ? 0 : (topic.solvedCount / topic.totalCount) * 100;
    const score = (topic.acRate ?? 0) * AC_WEIGHT + coverage * COVERAGE_WEIGHT;
    return {
      topicId: topic.topicId,
      topicName: topic.topicName,
      score: Math.round(Math.max(0, Math.min(100, score))),
    };
  });
}

/**
 * The radar plots the RADAR_AXIS_COUNT WEAKEST topics (owner, 2026-10-13: opening the dashboard must show
 * what to improve). Keeping the strongest, as before, dropped the true weakest topic and made the greeting
 * name a different one than the "Nên ưu tiên" block. Axes go back to the caller's order: a radar whose axes
 * are sorted by value draws a spiral, which reads as a trend that isn't there.
 */
export function deriveSkillRadar(topics: TopicProgress[]): SkillRadarPoint[] {
  const scored = scoreTopics(topics);
  const keep = new Set(
    [...scored]
      .sort((left, right) => left.score - right.score)
      .slice(0, RADAR_AXIS_COUNT)
      .map((point) => point.topicId),
  );
  return scored.filter((point) => keep.has(point.topicId));
}

/** The weakest of the given topics: pass `scoreTopics(...)`, not the radar subset (dc.html:102-103). */
export function pickWeakestTopic(
  points: SkillRadarPoint[],
): SkillRadarPoint | null {
  if (points.length === 0) return null;
  return points.reduce((weakest, point) =>
    point.score < weakest.score ? point : weakest,
  );
}
