// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Half-circle gauge, N colored slices summing to 100% — used for the 5-verdict distribution block
// (AC/WA/TLE/RE/CE, 02-bd/screens/admin/admin_overview.md section 2 point 3). Generic slice list in,
// no domain type — shared/ui.
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
  return { x: CENTER + RADIUS * Math.cos(rad), y: CENTER - RADIUS * Math.sin(rad) };
}

/** Sweeps clockwise on screen from 180deg (left) down to 0deg (right), passing through the top. */
function arcPath(startAngle: number, endAngle: number): string {
  const start = polar(startAngle);
  const end = polar(endAngle);
  const largeArc = startAngle - endAngle > 180 ? 1 : 0;
  return `M ${start.x} ${start.y} A ${RADIUS} ${RADIUS} 0 ${largeArc} 1 ${end.x} ${end.y}`;
}

export function HalfDonutGauge({ slices }: { slices: GaugeSlice[] }) {
  const total = slices.reduce((sum, slice) => sum + slice.value, 0) || 1;

  // Built with reduce (not a `let cursor` mutated across `.map`) — react-hooks/immutability
  // flags reassigning a variable captured by a render-time closure.
  const segments = slices.reduce<Array<GaugeSlice & { startAngle: number; endAngle: number; percent: number }>>(
    (acc, slice) => {
      const cursor = acc.length > 0 ? acc[acc.length - 1]!.endAngle : 180;
      const sweep = (slice.value / total) * 180;
      const endAngle = cursor - sweep;
      acc.push({ ...slice, startAngle: cursor, endAngle, percent: Math.round((slice.value / total) * 100) });
      return acc;
    },
    [],
  );

  return (
    <div>
      <svg viewBox="0 0 120 68" className="mx-auto w-full max-w-[170px]" role="img" aria-label="Kết quả chấm">
        {segments.map((segment) => (
          <path
            key={segment.label}
            d={arcPath(segment.startAngle, segment.endAngle)}
            fill="none"
            stroke={`var(${segment.colorVar})`}
            strokeWidth={STROKE_WIDTH}
            strokeLinecap="butt"
          />
        ))}
      </svg>
      <ul className="mt-2 grid grid-cols-2 gap-x-3 gap-y-1 text-xs">
        {segments.map((segment) => (
          <li key={segment.label} className="flex items-center gap-1.5 text-[var(--color-text-muted)]">
            <span className="h-2 w-2 rounded-full" style={{ background: `var(${segment.colorVar})` }} aria-hidden="true" />
            {segment.label} — {segment.percent}%
          </li>
        ))}
      </ul>
    </div>
  );
}
