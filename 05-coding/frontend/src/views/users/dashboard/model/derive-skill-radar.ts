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
// entities is exactly what the layer above them is for. Since DEC-2026-1123-student-dashboard-trim removed the
// radar, only the greeting reads it; the file name is kept to avoid churn.
//
// The weighting is locked by the owner (2026-10-18, DEC-2026-1018-usr-open-questions-phase2-5): 70% AC
// rate plus 30% solved ratio of the topic, with NO difficulty weight (difficulty is an admin-managed
// catalogue with no logic attached). Why those two terms: AC rate alone rates a student who solved 2
// easy problems at 100, and coverage alone ignores whether they got them right.
import type { TopicProgress } from "@/entities/progress";
import type { SkillRadarPoint } from "../api";

const AC_WEIGHT = 0.7;
const COVERAGE_WEIGHT = 0.3;

/** Every topic scored, in the caller's order. The greeting reads this (the radar was removed 2026-11-23). */
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

/** The weakest of the given topics: pass `scoreTopics(...)` over every topic (dc.html:102-103). */
export function pickWeakestTopic(
  points: SkillRadarPoint[],
): SkillRadarPoint | null {
  const [first] = points;
  if (!first) return null;
  return points.reduce(
    (weakest, point) => (point.score < weakest.score ? point : weakest),
    first,
  );
}
