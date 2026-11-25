// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// A handful of labelled columns side by side ("compare 3 to 6 things at a glance"): value on top, bar, a
// two-line label under it. Meant for few items; for a time series use StackedBarChart. Generic items in, no
// domain type, so it lives in shared/ui. Each item is a list entry, so the label and value stay readable by
// assistive technology and the full label is in the tooltip when the two-line clamp cuts it.
import { cn } from "@/shared/lib";

type ColumnItem = {
  label: string;
  value: number;
  /** Shown above the bar instead of `value` (e.g. "81%"). */
  valueLabel?: string;
  /** CSS custom-property name; falls back to `accentColorVar`. */
  colorVar?: string;
};

export function ColumnChart({
  items,
  max,
  height = 96,
  accentColorVar = "--color-primary",
  className,
}: {
  items: ColumnItem[];
  /** Value that fills the whole bar height; defaults to the largest value (use 100 for percentages). */
  max?: number;
  /** Bar area minimum height in px; the bars grow past it when the parent gives the chart more height. */
  height?: number;
  accentColorVar?: string;
  className?: string;
}) {
  const top = Math.max(max ?? Math.max(...items.map((item) => item.value)), 1);

  return (
    <ul className={cn("flex gap-3", className)}>
      {items.map((item) => (
        <li
          key={item.label}
          className="flex min-w-0 flex-1 flex-col items-center gap-1"
          title={item.label}
        >
          <span className="font-mono text-[11.5px] text-[var(--color-text-muted)]">
            {item.valueLabel ?? item.value}
          </span>
          <div
            className="flex w-full flex-1 items-end justify-center"
            style={{ minHeight: height }}
          >
            <div
              className="w-full max-w-[40px] rounded-t-md"
              style={{
                height: `${Math.min(100, (item.value / top) * 100)}%`,
                minHeight: 3,
                background: `var(${item.colorVar ?? accentColorVar})`,
              }}
            />
          </div>
          <span className="line-clamp-2 w-full text-center text-[11px] leading-tight break-words text-[var(--color-text)]">
            {item.label}
          </span>
        </li>
      ))}
    </ul>
  );
}
