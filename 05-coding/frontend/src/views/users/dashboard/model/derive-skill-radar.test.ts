import { describe, expect, it } from "vitest";
import type { TopicProgress } from "@/entities/progress";
import {
  deriveSkillRadar,
  pickWeakestTopic,
  RADAR_AXIS_COUNT,
  scoreTopics,
} from "./derive-skill-radar";

function topic(
  id: string,
  solved: number,
  total: number,
  acRate: number | null,
): TopicProgress {
  return {
    topicId: id,
    topicName: id,
    solvedCount: solved,
    totalCount: total,
    acRate,
    lastSubmittedAt: null,
  };
}

describe("deriveSkillRadar", () => {
  it("scores from AC rate and coverage, clamped to 0-100", () => {
    // 80 AC * 0.7 + 50 coverage * 0.3 = 71
    expect(deriveSkillRadar([topic("a", 5, 10, 80)])[0]!.score).toBe(71);
    // A topic never submitted to (acRate null) still scores on coverage alone, not NaN.
    expect(deriveSkillRadar([topic("b", 0, 10, null)])[0]!.score).toBe(0);
  });

  it("plots the RADAR_AXIS_COUNT weakest topics and restores the caller's order", () => {
    const topics = [
      ...Array.from({ length: RADAR_AXIS_COUNT }, (_, i) =>
        topic(`weak-${i}`, 2, 10, 30 + i),
      ),
      topic("strongest", 9, 10, 95),
    ];
    const points = deriveSkillRadar(topics);

    expect(points).toHaveLength(RADAR_AXIS_COUNT);
    // The strongest of 7 is dropped, and what remains is NOT sorted by score — a radar whose axes
    // are value-sorted draws a spiral that reads as a trend.
    expect(points.map((p) => p.topicId)).toEqual(
      topics.slice(0, RADAR_AXIS_COUNT).map((t) => t.topicId),
    );
  });
});

describe("scoreTopics", () => {
  it("keeps every topic, so the weakest is never lost to the radar's cut", () => {
    const topics = [
      ...Array.from({ length: RADAR_AXIS_COUNT }, (_, i) =>
        topic(`mid-${i}`, 5, 10, 60),
      ),
      topic("weakest", 1, 100, 10),
    ];
    expect(scoreTopics(topics)).toHaveLength(RADAR_AXIS_COUNT + 1);
    expect(pickWeakestTopic(scoreTopics(topics))?.topicId).toBe("weakest");
    // The radar plots it too: what to improve is on the chart.
    expect(deriveSkillRadar(topics).map((p) => p.topicId)).toContain("weakest");
  });
});

describe("pickWeakestTopic", () => {
  it("returns the lowest-scoring plotted topic, or null when nothing is plotted", () => {
    const points = scoreTopics([
      topic("strong", 9, 10, 90),
      topic("weak", 2, 10, 30),
    ]);
    expect(pickWeakestTopic(points)?.topicId).toBe("weak");
    expect(pickWeakestTopic([])).toBeNull();
  });
});
