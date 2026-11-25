// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// 6-month line chart with a total headline number (submissions-by-month block,
// 02-bd/screens/admin/ADM0101_overview.md section 3). Generic point list, no domain type — shared/ui.
type LinePoint = { label: string; value: number };

export function LineChartWithTotal({
  points,
  className,
  totalLabel,
  totalValue,
  accentColorVar = "--color-primary",
  chartClassName = "h-16",
  ariaLabel,
  area = false,
  smooth = false,
}: {
  points: LinePoint[];
  className?: string;
  totalLabel: string;
  totalValue: string;
  /** CSS custom-property name (e.g. "--color-admin-cyan"), read via var(). */
  accentColorVar?: string;
  /** Height of the plot; the default suits a short admin tile, a dashboard block passes a taller one. */
  chartClassName?: string;
  /** Accessible name of the plot; defaults to `totalLabel`, which alone does not say WHAT was counted. */
  ariaLabel?: string;
  /** Fill the region under the line with a faint tint of the accent. */
  area?: boolean;
  /** Join the measured points with a curve that stays within each pair's value range. */
  smooth?: boolean;
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
  const curve = smooth
    ? coords
        .map(({ x, y }, index) => {
          const previous = coords[index - 1];
          if (!previous) return `M ${x},${y}`;
          const middle = (previous.x + x) / 2;
          return `C ${middle},${previous.y} ${middle},${y} ${x},${y}`;
        })
        .join(" ")
    : "";

  return (
    <div className={className}>
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
        {area && curve && (
          <path
            d={`${curve} L ${coords.at(-1)?.x ?? 0},40 L 0,40 Z`}
            fill={`var(${accentColorVar})`}
            fillOpacity="0.15"
          />
        )}
        {area && !curve && (
          <polygon
            points={`0,40 ${polylinePoints} 100,40`}
            fill={`var(${accentColorVar})`}
            fillOpacity="0.15"
          />
        )}
        {curve ? (
          <path
            d={curve}
            fill="none"
            stroke={`var(${accentColorVar})`}
            strokeWidth="2"
            vectorEffect="non-scaling-stroke"
          />
        ) : (
          <polyline
            points={polylinePoints}
            fill="none"
            stroke={`var(${accentColorVar})`}
            strokeWidth="1.5"
            // The viewBox is stretched to the box (preserveAspectRatio none), which would draw the line thick
            // where it runs steep and thin where it runs flat; this keeps the stroke 1.5px everywhere.
            vectorEffect="non-scaling-stroke"
          />
        )}
        {/* The same stretch turns a dot into an ellipse, which a dense series (a month of days) makes
            look like ink blots, so dots are drawn only for a short series. */}
        {!area &&
          points.length <= 12 &&
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
