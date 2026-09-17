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

// BD component inventory explicitly flags this as a required addition, not optional: "trục hoành
// cần nhãn thời gian (thiếu ở prototype, phải bổ sung khi build FE)"
// (02-bd/screens/admin/admin_overview.md section 3). `pointLabels` was already threaded through as
// data (used for `title`/`aria-label`) but never rendered as visible text — this is what closes
// that gap. With up to 20 points (dc.html's "20 mốc"), printing all 20 under a narrow mid-column
// widget would overlap into an unreadable smear, so only every Nth label is drawn (plus the last
// point, so the axis never appears to stop short of the data) — the skipped labels stay reachable
// via each bar-group's `title` tooltip, same as before.
const MAX_VISIBLE_AXIS_LABELS = 6;

export function HorizontalBarChart({ series, pointLabels }: HorizontalBarChartProps) {
  const max = Math.max(...series.flatMap((s) => s.points), 1);
  const labelStep = Math.max(1, Math.ceil(pointLabels.length / MAX_VISIBLE_AXIS_LABELS));

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
      {/* Each point still gets a flex-1 anchor slot so a visible label lines up under its own bar
          group, but slots are ~16px wide at 20 points — too narrow for even "N13" without
          `overflow-visible` (a `truncate` here just turns every real label into "N..."). Neighbours
          are empty spacers, so the overflow has room to spill without colliding with anything. */}
      <div className="mt-1 flex gap-1" aria-hidden="true">
        {pointLabels.map((label, pointIndex) => {
          const isLastPoint = pointIndex === pointLabels.length - 1;
          const showLabel = pointIndex % labelStep === 0 || isLastPoint;
          return (
            <span
              key={label}
              className={`relative flex-1 overflow-visible whitespace-nowrap text-[10px] text-[var(--color-text-muted)] ${isLastPoint ? "text-right" : "text-center"}`}
            >
              {showLabel ? label : ""}
            </span>
          );
        })}
      </div>
    </div>
  );
}
