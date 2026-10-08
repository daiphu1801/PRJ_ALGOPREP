// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// 52-week x 7-day activity grid, ported from 09-layoutBase/Dashboard AlgoPrep.dc.html:188-213 (same
// 11px cells, 3px gaps, 5 intensity levels, legend reading Ít -> Nhiều, month ticks underneath).
// Takes a plain level grid with no domain type, so it lives in shared/ui per
// 01-rd/system/SYS0102_frontend_architecture.md section 2.A-B.
//
// Colour ramp comes from the app's own tokens rather than the prototype's hard-coded hexes, which
// only had a dark-mode branch written out (dc.html `heatColors`); `color-mix` against
// `--color-primary` gives both themes one ramp that tracks the theme instead of two hand-picked
// lists that drift.
import { cn } from "@/shared/lib";

type HeatmapCell = {
  /** Stable key and tooltip subject, e.g. an ISO date. */
  key: string;
  level: 0 | 1 | 2 | 3 | 4;
  /** Native title tooltip, e.g. "2026-09-14: 4 bài nộp". */
  title: string;
};

const LEVEL_STYLE: Record<HeatmapCell["level"], string> = {
  0: "bg-[var(--color-track)]",
  1: "bg-[color-mix(in_srgb,var(--color-primary)_28%,transparent)]",
  2: "bg-[color-mix(in_srgb,var(--color-primary)_50%,transparent)]",
  3: "bg-[color-mix(in_srgb,var(--color-primary)_74%,transparent)]",
  4: "bg-[var(--color-primary)]",
};

const LEGEND_LEVELS: HeatmapCell["level"][] = [0, 1, 2, 3, 4];

/** Cell size and gap are fixed (dc.html:203) and the month-tick row below is measured from them. */
const CELL_PX = 11;
const GAP_PX = 3;

export function ActivityHeatmap({
  weeks,
  monthLabels,
  lessLabel,
  moreLabel,
  ariaLabel,
}: {
  /** Oldest week first; each week is 7 cells. */
  weeks: HeatmapCell[][];
  monthLabels: string[];
  lessLabel: string;
  moreLabel: string;
  ariaLabel: string;
}) {
  return (
    <div>
      <div className="mb-2 flex items-center justify-end gap-1.5">
        <span className="text-[11.5px] text-[var(--color-text-subtle)]">
          {lessLabel}
        </span>
        {LEGEND_LEVELS.map((level) => (
          <span
            key={level}
            aria-hidden="true"
            className={cn("h-[11px] w-[11px] rounded-sm", LEVEL_STYLE[level])}
          />
        ))}
        <span className="text-[11.5px] text-[var(--color-text-subtle)]">
          {moreLabel}
        </span>
      </div>

      {/* The grid is decoration over a figure already stated in words above it (the "N ngày có bài
          nộp" summary), so it is one labelled group rather than 364 focusable cells. */}
      <div
        role="img"
        aria-label={ariaLabel}
        className="flex gap-[3px] overflow-x-auto pb-1"
      >
        {weeks.map((week, index) => (
          <div
            key={week[0]?.key ?? index}
            className="grid shrink-0 grid-rows-7 gap-[3px]"
          >
            {week.map((cell) => (
              <span
                key={cell.key}
                title={cell.title}
                className={cn(
                  "h-[11px] w-[11px] rounded-sm",
                  LEVEL_STYLE[cell.level],
                )}
              />
            ))}
          </div>
        ))}
      </div>

      {/* Sized to the grid, not to the card: the cells are a fixed 11px + 3px gap, so a tick row
          measured in percent of the container drifts away from the columns it labels as soon as
          the card is wider than the grid. */}
      <div
        className="mt-1.5 flex"
        style={{ width: weeks.length * (CELL_PX + GAP_PX) - GAP_PX }}
        aria-hidden="true"
      >
        {monthLabels.map((label) => (
          <span
            key={label}
            className="flex-1 font-mono text-[11px] text-[var(--color-text-subtle)]"
          >
            {label}
          </span>
        ))}
      </div>
    </div>
  );
}
