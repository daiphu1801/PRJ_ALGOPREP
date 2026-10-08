// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
// One chart group per admin-managed problem level, in admin order. A level with no data gets 0 / 0;
// data for a level that no longer exists is dropped. Bar colours stay per series, not per level.
import type { ProblemLevel } from "@/entities/problem";
import type { DifficultyBreakdown } from "./types";

export function buildDifficultyGroups(
  levels: readonly ProblemLevel[],
  data: DifficultyBreakdown,
) {
  return levels.map((level) => {
    const row = data.levels.find((r) => r.key === level.key);
    return {
      label: level.label,
      bars: [
        {
          label: "Tổng",
          value: row?.total ?? 0,
          colorVar: "--color-admin-slate",
        },
        {
          label: "AC",
          value: row?.accepted ?? 0,
          colorVar: "--color-admin-cyan",
        },
      ],
    };
  });
}
