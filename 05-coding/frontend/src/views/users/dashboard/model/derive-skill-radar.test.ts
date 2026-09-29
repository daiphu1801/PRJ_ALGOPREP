import { describe, expect, it } from "vitest";
import type { TopicProgress } from "@/entities/progress";
import { deriveSkillRadar, pickWeakestTopic, RADAR_AXIS_COUNT } from "./derive-skill-radar";

function topic(id: string, solved: number, total: number, acRate: number | null): TopicProgress {
  return { topicId: id, topicName: id, solvedCount: solved, totalCount: total, acRate, lastSubmittedAt: null };
}

describe("deriveSkillRadar", () => {
  it("scores from AC rate and coverage, clamped to 0-100", () => {
    // 80 AC * 0.7 + 50 coverage * 0.3 = 71
    expect(deriveSkillRadar([topic("a", 5, 10, 80)])[0]!.score).toBe(71);
    // A topic never submitted to (acRate null) still scores on coverage alone, not NaN.
    expect(deriveSkillRadar([topic("b", 0, 10, null)])[0]!.score).toBe(0);
  });

  it("keeps only the strongest RADAR_AXIS_COUNT topics but restores the caller's order", () => {
    const topics = [
      topic("weakest", 1, 100, 10),
      ...Array.from({ length: RADAR_AXIS_COUNT }, (_, i) => topic(`strong-${i}`, 9, 10, 90)),
    ];
    const points = deriveSkillRadar(topics);

    expect(points).toHaveLength(RADAR_AXIS_COUNT);
    // The weakest of 7 is dropped, and what remains is NOT sorted by score — a radar whose axes
    // are value-sorted draws a spiral that reads as a trend.
    expect(points.map((p) => p.topicId)).toEqual(topics.slice(1).map((t) => t.topicId));
  });
});

describe("pickWeakestTopic", () => {
  it("returns the lowest-scoring plotted topic, or null when nothing is plotted", () => {
    const points = deriveSkillRadar([topic("strong", 9, 10, 90), topic("weak", 2, 10, 30)]);
    expect(pickWeakestTopic(points)?.topicId).toBe("weak");
    expect(pickWeakestTopic([])).toBeNull();
  });
});
