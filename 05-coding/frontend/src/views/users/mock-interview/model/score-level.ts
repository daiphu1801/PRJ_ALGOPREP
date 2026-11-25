// Three bands of a 0 to 10 score, the same cut points the screen already coloured with (5 and 8).
export type ScoreLevel = "weak" | "fair" | "good";

export const SCORE_LEVELS: readonly ScoreLevel[] = ["weak", "fair", "good"];

export const SCORE_LEVEL_COLOR: Record<ScoreLevel, string> = {
  weak: "var(--color-admin-negative)",
  fair: "var(--color-admin-warn)",
  good: "var(--color-success)",
};

export function scoreLevel(score: number): ScoreLevel {
  if (score >= 8) return "good";
  if (score >= 5) return "fair";
  return "weak";
}
