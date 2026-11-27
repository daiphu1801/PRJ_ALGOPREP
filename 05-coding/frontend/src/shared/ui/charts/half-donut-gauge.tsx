// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Half-circle gauge, N colored slices summing to 100% — used for the 5-verdict distribution block
// (AC/WA/TLE/RE/CE, 02-bd/screens/admin/ADM0101_overview.md section 2 point 3). Generic slice list in,
// no domain type — shared/ui.
import type { ReactNode } from "react";

type GaugeSlice = {
  label: string;
  value: number;
  colorVar: string;
};

const CENTER = 60;
const RADIUS = 46;
const STROKE_WIDTH = 16;

function polar(angleDeg: number) {
  const rad = (angleDeg * Math.PI) / 180;
  return {
    x: CENTER + RADIUS * Math.cos(rad),
    y: CENTER - RADIUS * Math.sin(rad),
  };
}

/** Sweeps clockwise on screen from 180deg (left) down to 0deg (right), passing through the top. */
function arcPath(startAngle: number, endAngle: number): string {
  const start = polar(startAngle);
  const end = polar(endAngle);
  const largeArc = startAngle - endAngle > 180 ? 1 : 0;
  return `M ${start.x} ${start.y} A ${RADIUS} ${RADIUS} 0 ${largeArc} 1 ${end.x} ${end.y}`;
}

/**
 * Whole-number percents that always add up to 100: floor every share, then hand the leftover points
 * to the largest remainders (BD ADM0101 :485 asks for the remainder to go to the biggest slice; with
 * ties that is the same thing). Rounding each slice alone could print 99 or 101 (UX review B5).
 */
export function wholePercents(values: number[]): number[] {
  const total = values.reduce((sum, value) => sum + value, 0);
  if (total === 0) return values.map(() => 0);
  const exact = values.map((value) => (value / total) * 100);
  const floors = exact.map(Math.floor);
  let leftover = 100 - floors.reduce((sum, value) => sum + value, 0);
  const byRemainder = exact
    .map((value, index) => ({ index, remainder: value - floors[index]! }))
    .sort(
      (a, b) =>
        b.remainder - a.remainder || values[b.index]! - values[a.index]!,
    );
  for (const { index } of byRemainder) {
    if (leftover <= 0) break;
    floors[index]! += 1;
    leftover -= 1;
  }
  return floors;
}

export function HalfDonutGauge({
  slices,
  chartClassName = "max-w-[170px]",
  fullCircle = false,
  center,
}: {
  slices: GaugeSlice[];
  chartClassName?: string;
  fullCircle?: boolean;
  /** Content drawn inside the arc (half circle only), e.g. a headline percentage. */
  center?: ReactNode;
}) {
  const total = slices.reduce((sum, slice) => sum + slice.value, 0) || 1;
  const percents = wholePercents(slices.map((slice) => slice.value));

  // Built with reduce (not a `let cursor` mutated across `.map`) — react-hooks/immutability
  // flags reassigning a variable captured by a render-time closure.
  const segments = slices.reduce<
    Array<
      GaugeSlice & { startAngle: number; endAngle: number; percent: number }
    >
  >((acc, slice, index) => {
    const cursor =
      acc.length > 0 ? acc[acc.length - 1]!.endAngle : fullCircle ? 90 : 180;
    const sweep = (slice.value / total) * (fullCircle ? 360 : 180);
    const endAngle = cursor - sweep;
    acc.push({
      ...slice,
      startAngle: cursor,
      endAngle,
      percent: percents[index]!,
    });
    return acc;
  }, []);

  return (
    <div>
      <div className={`relative mx-auto w-full ${chartClassName}`}>
        <svg
          viewBox={fullCircle ? "0 0 120 120" : "0 0 120 68"}
          className="w-full"
          role="img"
          aria-label="Kết quả chấm"
        >
          {segments.map((segment) =>
            fullCircle && segment.value === total ? (
              <circle
                key={segment.label}
                cx={CENTER}
                cy={CENTER}
                r={RADIUS}
                fill="none"
                stroke={`var(${segment.colorVar})`}
                strokeWidth={STROKE_WIDTH}
              />
            ) : (
              <path
                key={segment.label}
                d={arcPath(segment.startAngle, segment.endAngle)}
                fill="none"
                stroke={`var(${segment.colorVar})`}
                strokeWidth={STROKE_WIDTH}
                strokeLinecap="butt"
              />
            ),
          )}
        </svg>
        {center && !fullCircle ? (
          // The arc baseline sits 8 of 68 viewBox units above the bottom edge.
          <div className="absolute inset-x-0 bottom-[12%] text-center">
            {center}
          </div>
        ) : null}
      </div>
      <ul className="mt-2 grid grid-cols-2 gap-x-3 gap-y-1 text-xs">
        {segments.map((segment) => (
          <li
            key={segment.label}
            className="flex items-center gap-1.5 text-[var(--color-text-muted)]"
          >
            <span
              className="size-2 shrink-0 rounded-full"
              style={{ background: `var(${segment.colorVar})` }}
              aria-hidden="true"
            />
            {segment.label} — {segment.percent}%
          </li>
        ))}
      </ul>
    </div>
  );
}
