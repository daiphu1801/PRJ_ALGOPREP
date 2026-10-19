// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// 6-month line chart with a total headline number (submissions-by-month block,
// 02-bd/screens/admin/ADM0101_overview.md section 3). Generic point list, no domain type — shared/ui.
type LinePoint = { label: string; value: number };

export function LineChartWithTotal({
  points,
  totalLabel,
  totalValue,
  accentColorVar = "--color-primary",
  chartClassName = "h-16",
  ariaLabel,
}: {
  points: LinePoint[];
  totalLabel: string;
  totalValue: string;
  /** CSS custom-property name (e.g. "--color-admin-cyan"), read via var(). */
  accentColorVar?: string;
  /** Height of the plot; the default suits a short admin tile, a dashboard block passes a taller one. */
  chartClassName?: string;
  /** Accessible name of the plot; defaults to `totalLabel`, which alone does not say WHAT was counted. */
  ariaLabel?: string;
}) {
  const values = points.map((p) => p.value);
  const max = Math.max(...values, 1);
  const min = Math.min(...values, 0);
  const range = max - min || 1;
  const coords = points.map((point, index) => {
    const x = (index / (points.length - 1 || 1)) * 100;
    const y = 40 - ((point.value - min) / range) * 36 - 2;
    return { x, y };
  });
  const polylinePoints = coords.map((c) => `${c.x},${c.y}`).join(" ");

  return (
    <div>
      <p className="text-2xl font-semibold text-[var(--color-text)]">
        {totalValue}
      </p>
      <p className="mb-2 text-xs text-[var(--color-text-muted)]">
        {totalLabel}
      </p>
      <svg
        viewBox="0 0 100 40"
        className={`${chartClassName} w-full`}
        role="img"
        aria-label={ariaLabel ?? totalLabel}
        preserveAspectRatio="none"
      >
        <polyline
          points={polylinePoints}
          fill="none"
          stroke={`var(${accentColorVar})`}
          strokeWidth="1.5"
          // The viewBox is stretched to the box (preserveAspectRatio none), which would draw the line thick
          // where it runs steep and thin where it runs flat; this keeps the stroke 1.5px everywhere.
          vectorEffect="non-scaling-stroke"
        />
        {/* The same stretch turns a dot into an ellipse, which a dense series (a month of days) makes
            look like ink blots, so dots are drawn only for a short series. */}
        {points.length <= 12 &&
          coords.map((c, index) => (
            <circle
              key={points[index]!.label}
              cx={c.x}
              cy={c.y}
              r="1.4"
              fill={`var(${accentColorVar})`}
            />
          ))}
      </svg>
      <div className="mt-1 flex justify-between text-[10px] text-[var(--color-text-muted)]">
        {points.map((p) => (
          <span key={p.label}>{p.label}</span>
        ))}
      </div>
    </div>
  );
}
