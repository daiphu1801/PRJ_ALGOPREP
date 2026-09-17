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
};

export function StatCard({
  label,
  value,
  delta,
  meta,
  valueColorVar,
  className,
}: StatCardProps) {
  return (
    <div
      className={cn("glass-card border border-[var(--color-border)] px-[18px] py-4", className)}
    >
      <p className="mb-2.5 text-xs font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
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
          <span className="text-[12.5px] font-semibold text-[var(--color-text-muted)]">{delta}</span>
        ) : null}
      </p>
      {meta ? (
        <p className="mt-1.5 text-[12.5px] text-[var(--color-text-muted)]">{meta}</p>
      ) : null}
    </div>
  );
}
