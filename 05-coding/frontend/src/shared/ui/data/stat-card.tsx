// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Plain figure tile: uppercase label, big number, delta beside it, one line of context underneath.
// 09-layoutBase/Admin - Nhật ký hệ thống.dc.html:165-176 — 12px uppercase label with 0.08em
// tracking, 27px value, 12.5px delta and meta.
//
// Distinct from StatCardWithSparkline in the same folder, which additionally plots a series; the
// Admin screens that show a bare figure (system log, ai usage, queue monitor) have no series to
// plot, and passing a fake one to reuse that component would invent data.
import type { ReactNode } from "react";
import type { LucideIcon } from "lucide-react";
import { cn } from "@/shared/lib";

type StatCardProps = {
  label: ReactNode;
  value: ReactNode;
  /** Change against the previous period, e.g. "+9". Rendered as-is; the caller formats and signs it. */
  delta?: ReactNode;
  /** One line of context under the figure. */
  meta?: ReactNode;
  /** CSS custom-property name (e.g. "--color-admin-warn") colouring the value. */
  valueColorVar?: string;
  className?: string;
  /**
   * For a tile that already sits inside another card (DashboardBlockState): drops the glass chrome so the
   * tile does not draw a second box in the first, and gives the three lines more air. Default tiles keep
   * their look.
   */
  bare?: boolean;
  /** Optional tinted icon chip in the top-right corner (decorative: the label already names the figure). */
  icon?: LucideIcon;
  /** CSS custom-property name colouring the icon chip; defaults to the generic accent. */
  iconColorVar?: string;
};

export function StatCard({
  label,
  value,
  delta,
  meta,
  valueColorVar,
  className,
  bare = false,
  icon: Icon,
  iconColorVar = "--color-primary",
}: StatCardProps) {
  return (
    <div
      className={cn(
        "relative",
        bare
          ? "px-2 py-1.5"
          : "glass-card border border-[var(--color-border)] px-[18px] py-4",
        className,
      )}
    >
      {Icon ? (
        <span
          aria-hidden="true"
          className="absolute top-4 right-[18px] flex h-9 w-9 items-center justify-center rounded-xl"
          style={{
            color: `var(${iconColorVar})`,
            background: `color-mix(in srgb, var(${iconColorVar}) 14%, transparent)`,
          }}
        >
          <Icon className="h-[18px] w-[18px]" />
        </span>
      ) : null}
      <p
        className={cn(
          "text-xs font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase",
          bare ? "mb-4" : "mb-2.5",
        )}
      >
        {label}
      </p>
      <p className="flex items-baseline gap-2">
        <span
          className="text-[27px] font-bold tracking-[-0.02em]"
          style={valueColorVar ? { color: `var(${valueColorVar})` } : undefined}
        >
          {value}
        </span>
        {delta ? (
          <span className="text-[12.5px] font-semibold text-[var(--color-text-muted)]">
            {delta}
          </span>
        ) : null}
      </p>
      {meta ? (
        <p
          className={cn(
            "text-[12.5px] text-[var(--color-text-muted)]",
            bare ? "mt-3" : "mt-1.5",
          )}
        >
          {meta}
        </p>
      ) : null}
    </div>
  );
}
