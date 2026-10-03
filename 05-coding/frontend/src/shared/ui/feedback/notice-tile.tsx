// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Tinted notice box: coloured title, muted detail line, background tinted by severity.
// 09-layoutBase/Admin - Token AI.dc.html:231-237 — 11px/13px padding, 14px radius, tint derived
// from the severity colour at ~10% alpha.
//
// role="status" rather than "alert": these render with the page rather than interrupting, so an
// assertive live region would be wrong. A screen that pushes a notice mid-session should pass its
// own role.
import type { ReactNode } from "react";
import { cn } from "@/shared/lib";

const TONE = {
  warn: {
    fg: "text-[var(--color-admin-warn)]",
    bg: "bg-[color-mix(in_srgb,var(--color-admin-warn)_11%,transparent)]",
  },
  negative: {
    fg: "text-[var(--color-admin-negative)]",
    bg: "bg-[color-mix(in_srgb,var(--color-admin-negative)_9%,transparent)]",
  },
  info: {
    fg: "text-[var(--color-accent-blue)]",
    bg: "bg-[var(--color-accent-blue-bg)]",
  },
  success: {
    fg: "text-[var(--color-success-text)]",
    bg: "bg-[color-mix(in_srgb,var(--color-success)_11%,transparent)]",
  },
} as const;

export type NoticeTone = keyof typeof TONE;

export function NoticeTile({
  tone = "info",
  title,
  children,
  className,
}: {
  tone?: NoticeTone;
  title: ReactNode;
  children?: ReactNode;
  className?: string;
}) {
  return (
    <div
      role="status"
      className={cn(
        "rounded-2xl border border-[var(--color-border)] px-3 py-[11px]",
        TONE[tone].bg,
        className,
      )}
    >
      <p className={cn("text-[13px] font-semibold", TONE[tone].fg)}>{title}</p>
      {children ? (
        <p className="mt-0.5 text-[12.5px] text-pretty text-[var(--color-text-muted)]">{children}</p>
      ) : null}
    </div>
  );
}
