// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Stacked columns: one column per period, segments piled by series.
// 09-layoutBase/Admin - Token AI.dc.html:192-205 — 240px plot, 7px gaps, segments stacked bottom-up
// with only the top one rounded, a bottom rule, and date labels beneath.
//
// Distinct from GroupedBarChart in this folder, which places a group's bars SIDE BY SIDE. That
// answers "compare A vs B per group"; this one answers "what does the total break down into".
import { cn } from "@/shared/lib";

export type StackedSeries = {
  key: string;
  label: string;
  /** CSS custom-property name, e.g. "--color-admin-teal". */
  colorVar: string;
};

export type StackedBar = {
  label: string;
  /** Tooltip text — the mockup puts the date plus the total here. */
  title?: string;
  /** Value per series key. Missing keys count as zero. */
  values: Record<string, number>;
};

export function StackedBarChart({
  series,
  bars,
  height = 240,
  className,
}: {
  series: StackedSeries[];
  bars: StackedBar[];
  height?: number;
  className?: string;
}) {
  const totals = bars.map((bar) =>
    series.reduce((sum, item) => sum + (bar.values[item.key] ?? 0), 0),
  );
  const max = Math.max(...totals, 1);

  return (
    <div className={className}>
      <div className="mb-3 flex flex-wrap gap-3.5 text-xs text-[var(--color-text-muted)]">
        {series.map((item) => (
          <span key={item.key} className="inline-flex items-center gap-1.5">
            <span
              aria-hidden="true"
              className="h-[9px] w-[9px] rounded-[3px]"
              style={{ background: `var(${item.colorVar})` }}
            />
            {item.label}
          </span>
        ))}
      </div>

      <div
        className="flex items-end gap-[7px] border-b border-[var(--color-border)] pt-2.5"
        style={{ height }}
      >
        {bars.map((bar) => (
          <div
            key={bar.label}
            title={bar.title}
            className="flex h-full min-w-0 flex-1 flex-col justify-end gap-0.5"
          >
            {/* Reversed so the first series ends up on top, matching the legend's reading order. */}
            {[...series].reverse().map((item, index) => {
              const value = bar.values[item.key] ?? 0;
              if (value <= 0) return null;
              return (
                <div
                  key={item.key}
                  style={{
                    height: `${(value / max) * 100}%`,
                    background: `var(${item.colorVar})`,
                  }}
                  className={cn(index === series.length - 1 && "rounded-t-[5px]")}
                />
              );
            })}
          </div>
        ))}
      </div>

      <div className="mt-2 flex gap-[7px]">
        {bars.map((bar) => (
          <div
            key={bar.label}
            className="min-w-0 flex-1 text-center font-mono text-[10.5px] text-[var(--color-text-subtle)]"
          >
            {bar.label}
          </div>
        ))}
      </div>
    </div>
  );
}
