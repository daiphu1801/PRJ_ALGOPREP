// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Radar/spider chart, ported from 09-layoutBase/Dashboard AlgoPrep.dc.html:160-184 (same centre
// 150,126, same radius 92, same 4 rings at 0.25/0.5/0.75/1, same label offset of 22px outside the
// outer ring). Takes a plain {label, value} list on a fixed 0-100 scale — no domain type — so it
// belongs in shared/ui per 01-rd/system/SYS0102_frontend_architecture.md section 2.A-B.
//
// Built here rather than pulled from a charting library: the whole component is the trigonometry
// below, and every chart already in this folder (donut, sparkline, stacked bar) is hand-rolled SVG
// for the same reason — adding a chart dependency for one screen would be the only one in the app.
type RadarPoint = {
  label: string;
  /** 0-100. Values outside the range are clamped rather than drawn outside the outer ring. */
  value: number;
};

const CENTRE_X = 150;
const CENTRE_Y = 126;
const RADIUS = 92;
const RINGS = [0.25, 0.5, 0.75, 1];

export function RadarChart({
  points,
  ariaLabel,
  accentColorVar = "--color-primary",
  emphasizeLowest = false,
}: {
  points: RadarPoint[];
  /** Required: an SVG with role="img" is invisible to a screen reader without one. */
  ariaLabel: string;
  accentColorVar?: string;
  /** Draws the lowest axis label in the warning colour, so the eye lands on what to improve first. */
  emphasizeLowest?: boolean;
}) {
  if (points.length < 3) return null;

  // -PI/2 puts the first axis straight up, matching the prototype.
  const angleAt = (index: number) =>
    (Math.PI * 2 * index) / points.length - Math.PI / 2;
  const pointAt = (index: number, fraction: number) => ({
    x: CENTRE_X + Math.cos(angleAt(index)) * RADIUS * fraction,
    y: CENTRE_Y + Math.sin(angleAt(index)) * RADIUS * fraction,
  });
  const ringPoints = (fraction: number) =>
    points
      .map((_, index) => {
        const p = pointAt(index, fraction);
        return `${p.x.toFixed(1)},${p.y.toFixed(1)}`;
      })
      .join(" ");

  const lowest = emphasizeLowest
    ? points.reduce(
        (low, point, index) => (point.value < points[low]!.value ? index : low),
        0,
      )
    : -1;
  const clamped = points.map((point) =>
    Math.max(0, Math.min(100, point.value)),
  );
  const shape = clamped
    .map((value, index) => {
      const p = pointAt(index, value / 100);
      return `${p.x.toFixed(1)},${p.y.toFixed(1)}`;
    })
    .join(" ");

  return (
    <svg
      // The viewBox is wider than the 300px the prototype drew in so a long label ("Heap / priority queue")
      // stays inside it on a phone, and starts above y=0 so the top label clears the text over the chart.
      viewBox="-50 -14 400 276"
      role="img"
      aria-label={ariaLabel}
      className="block h-auto w-full max-w-[400px] overflow-visible"
    >
      {RINGS.map((fraction) => (
        <polygon
          key={fraction}
          points={ringPoints(fraction)}
          fill="none"
          stroke="var(--color-border)"
          strokeWidth="1"
        />
      ))}
      {points.map((point, index) => {
        const outer = pointAt(index, 1);
        return (
          <line
            key={point.label}
            x1={CENTRE_X}
            y1={CENTRE_Y}
            x2={outer.x.toFixed(1)}
            y2={outer.y.toFixed(1)}
            stroke="var(--color-border)"
            strokeWidth="1"
          />
        );
      })}
      <polygon
        points={shape}
        fill={`var(${accentColorVar})`}
        fillOpacity="0.16"
        stroke={`var(${accentColorVar})`}
        strokeWidth="2"
      />
      {clamped.map((value, index) => {
        const p = pointAt(index, value / 100);
        return (
          <circle
            key={points[index]!.label}
            cx={p.x.toFixed(1)}
            cy={p.y.toFixed(1)}
            r="3"
            fill={`var(${accentColorVar})`}
          />
        );
      })}
      {points.map((point, index) => {
        // Label sits 22px beyond the outer ring; the nudges below stop the top and bottom labels
        // from sitting on the ring itself, and the anchor keeps left-side labels from overflowing
        // the viewBox (all three rules are dc.html's `radarLabels`).
        const cos = Math.cos(angleAt(index));
        const sin = Math.sin(angleAt(index));
        const x = CENTRE_X + cos * (RADIUS + 22);
        const y = CENTRE_Y + sin * (RADIUS + 22);
        const labelY = y + (sin > 0.4 ? 8 : sin < -0.4 ? -4 : 0);
        const valueY = y + (sin > 0.4 ? 21 : sin < -0.4 ? 9 : 13);
        const anchor = cos > 0.3 ? "start" : cos < -0.3 ? "end" : "middle";
        return (
          <g key={`${point.label}-label`}>
            <text
              x={x.toFixed(1)}
              y={labelY.toFixed(1)}
              textAnchor={anchor}
              fontSize="11.5"
              fontWeight={index === lowest ? "700" : "600"}
              fill={
                index === lowest
                  ? "var(--color-admin-warn)"
                  : "var(--color-text-muted)"
              }
            >
              {point.label}
            </text>
            <text
              x={x.toFixed(1)}
              y={valueY.toFixed(1)}
              textAnchor={anchor}
              fontSize="11"
              fill="var(--color-text-subtle)"
              className="font-mono"
            >
              {clamped[index]}
            </text>
          </g>
        );
      })}
    </svg>
  );
}
