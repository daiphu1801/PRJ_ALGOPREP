// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Single proportional bar on a sunken track. 09-layoutBase/Admin - Token AI.dc.html:213-215 (monthly
// budget, 10px tall) and Admin - Ngôn ngữ và giới hạn.dc.html:135-137 (go-judge queue, 6px).
//
// role="progressbar" with the real min/max/now: a screen reader then reads "74%" instead of nothing,
// which a bare styled <div> gives.
import { cn } from "@/shared/lib";

export function ProgressBar({
  value,
  max = 100,
  label,
  /** CSS custom-property name, or a full gradient string for the fill. */
  fill = "var(--color-admin-teal)",
  height = 10,
  className,
}: {
  value: number;
  max?: number;
  /** Accessible name — what this bar measures. */
  label: string;
  fill?: string;
  height?: number;
  className?: string;
}) {
  const percent = Math.max(0, Math.min(100, (value / max) * 100));

  return (
    <div
      role="progressbar"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={max}
      aria-valuenow={value}
      style={{ height }}
      className={cn("overflow-hidden rounded-full bg-[var(--color-track)]", className)}
    >
      <div style={{ width: `${percent}%`, background: fill }} className="h-full rounded-full" />
    </div>
  );
}
