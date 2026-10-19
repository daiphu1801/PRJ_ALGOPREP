import { describe, expect, it } from "vitest";
import { deriveFocusSuggestions } from "./derive-focus";
import type { TopicProgress } from "./types";

function topic(
  id: string,
  solved: number,
  total: number,
  acRate: number | null,
  lastSubmittedAt: string | null = "2026-09-01",
): TopicProgress {
  return {
    topicId: id,
    topicName: id,
    solvedCount: solved,
    totalCount: total,
    acRate,
    lastSubmittedAt,
  };
}

const NOW = new Date("2026-10-01");

describe("deriveFocusSuggestions", () => {
  const topics = [
    topic("low-ac", 5, 10, 20),
    topic("most-unsolved", 10, 50, 60),
    topic("stale", 8, 10, 70, "2026-01-01"),
    topic("weakest-skill", 1, 5, 40),
  ];

  it("keeps the three rules without a pinned topic", () => {
    expect(
      deriveFocusSuggestions(topics, NOW).map((s) => s.reason.code),
    ).toEqual(["lowestAcRate", "mostUnsolved", "leastRecentlySubmitted"]);
  });

  it("puts the pinned topic first and fills the other slots with the rules, never twice", () => {
    const result = deriveFocusSuggestions(topics, NOW, {
      topicId: "weakest-skill",
      score: 25,
    });

    expect(result).toHaveLength(3);
    expect(result[0]).toMatchObject({
      topicId: "weakest-skill",
      reason: { code: "weakestSkill", score: 25 },
    });
    expect(new Set(result.map((s) => s.topicId)).size).toBe(3);
  });

  it("ignores a pinned topic that is not in the list", () => {
    const result = deriveFocusSuggestions(topics, NOW, {
      topicId: "missing",
      score: 1,
    });
    expect(result[0]!.reason.code).toBe("lowestAcRate");
  });

  it("does not pin a weakest topic that has no unsolved problems left", () => {
    const solved = [topic("done", 5, 5, 90), topic("open", 1, 5, 30)];
    const result = deriveFocusSuggestions(solved, NOW, {
      topicId: "done",
      score: 95,
    });
    expect(result.map((s) => s.topicId)).toEqual(["open"]);
  });

  it("returns nothing when every topic is solved", () => {
    expect(
      deriveFocusSuggestions([topic("done", 5, 5, 90)], NOW, {
        topicId: "done",
        score: 95,
      }),
    ).toEqual([]);
  });

  it("never ranks a topic without an AC rate as 0%", () => {
    const result = deriveFocusSuggestions(
      [topic("no-data", 0, 5, null), topic("some", 1, 5, 50)],
      NOW,
    );
    expect(result[0]).toMatchObject({
      topicId: "some",
      reason: { code: "lowestAcRate" },
    });
  });
});
