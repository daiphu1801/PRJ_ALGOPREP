// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Multi-series bar chart (submissions-by-language block: 3 fixed series, one per submission
// language — 02-bd/screens/admin/admin_overview.md section 3). Takes only generic series/points,
// no domain type — shared/ui.
type Series = {
  label: string;
  colorVar: string;
  points: number[];
};

type HorizontalBarChartProps = {
  series: Series[];
  /** One label per time point on the x axis (day/week — left to DD to pin down, section 8 of BD). */
  pointLabels: string[];
};

export function HorizontalBarChart({ series, pointLabels }: HorizontalBarChartProps) {
  const max = Math.max(...series.flatMap((s) => s.points), 1);

  return (
    <div>
      <div className="mb-3 flex flex-wrap gap-3 text-xs text-[var(--color-text-muted)]">
        {series.map((s) => (
          <span key={s.label} className="flex items-center gap-1.5">
            <span className="h-2 w-2 rounded-full" style={{ background: `var(${s.colorVar})` }} aria-hidden="true" />
            {s.label}
          </span>
        ))}
      </div>
      <div className="flex h-20 items-end gap-1" role="img" aria-label={series.map((s) => s.label).join(", ")}>
        {pointLabels.map((label, pointIndex) => (
          <div key={label} className="flex flex-1 items-end gap-px" title={label}>
            {series.map((s) => (
              <div
                key={s.label}
                className="flex-1 rounded-t-sm"
                style={{
                  height: `${((s.points[pointIndex] ?? 0) / max) * 100}%`,
                  background: `var(${s.colorVar})`,
                  minHeight: 2,
                }}
              />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}
