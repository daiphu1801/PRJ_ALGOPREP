// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Presentational only (numbers + a label in, nothing about the AlgoPrep domain) — shared/ui per
// frontend_architecture.md section 2.A-B. No charting library added (none was in package.json);
// the sparkline is a tiny inline SVG polyline, good enough to show real shape/proportions
// (vibecode-pipeline PROTOTYPE lane, "not pixel-perfect").
type StatCardWithSparklineProps = {
  label: string;
  value: string;
  deltaPercent: number;
  deltaDirection: "up" | "down";
  sparklineSeries: number[];
  /** CSS custom-property name (e.g. "--color-admin-cyan"), read via var(). Defaults to the generic accent. */
  accentColorVar?: string;
};

export function StatCardWithSparkline({
  label,
  value,
  deltaPercent,
  deltaDirection,
  sparklineSeries,
  accentColorVar = "--color-primary",
}: StatCardWithSparklineProps) {
  const max = Math.max(...sparklineSeries, 1);
  const min = Math.min(...sparklineSeries, 0);
  const range = max - min || 1;
  const points = sparklineSeries
    .map((point, index) => {
      const x = (index / (sparklineSeries.length - 1 || 1)) * 100;
      const y = 24 - ((point - min) / range) * 24;
      return `${x},${y}`;
    })
    .join(" ");

  // dc.html:424-425 colors BOTH the delta text and the sparkline with the SAME series accent
  // (cyan for submissions, teal for active users), not a universal green-up/red-down convention —
  // kept 1:1 rather than substituting the more common pattern.
  const deltaLabel = Math.abs(deltaPercent).toLocaleString("vi-VN", {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
  });

  return (
    <div className="glass-card flex items-center justify-between gap-3 p-3">
      <div>
        <p className="text-xs text-[var(--color-text-muted)]">{label}</p>
        <p className="mt-1 text-2xl font-semibold text-[var(--color-text)]">{value}</p>
        <p className="mt-1 text-xs font-medium" style={{ color: `var(${accentColorVar})` }}>
          {deltaDirection === "up" ? "+" : "-"}
          {deltaLabel}%
        </p>
      </div>
      <svg viewBox="0 0 100 24" className="h-7 w-16 shrink-0" aria-hidden="true">
        <polyline
          points={points}
          fill="none"
          stroke={`var(${accentColorVar})`}
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    </div>
  );
}
