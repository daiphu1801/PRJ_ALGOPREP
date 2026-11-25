import { describe, expect, it } from "vitest";
import type { TopicProgress } from "@/entities/progress";
import { pickWeakestTopic, scoreTopics } from "./derive-skill-radar";

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

describe("scoreTopics", () => {
  it("scores from AC rate and coverage, clamped to 0-100, and keeps every topic", () => {
    // 80 x 0.7 + 50 x 0.3 = 71; no AC rate and nothing solved = 0.
    const points = scoreTopics([
      topic("a", 5, 10, 80),
      topic("b", 0, 10, null),
    ]);
    expect(points.map((p) => p.score)).toEqual([71, 0]);
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
