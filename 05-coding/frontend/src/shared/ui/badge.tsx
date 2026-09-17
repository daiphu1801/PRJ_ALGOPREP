// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Pill shape and metrics match 09-layoutBase/Admin - *.dc.html (border-radius: 999px appears in all
// 12 Admin mockups). Presentational only: it takes a `variant` and children, never a domain type —
// DifficultyBadge/StatusBadge/VerdictLabel take Problem/Submission and therefore belong in
// entities/*/ui per 01-rd/system/frontend_architecture.md section 2.A.
import type { ComponentPropsWithoutRef } from "react";
import { cn } from "@/shared/lib";

const VARIANT = {
  neutral: "bg-[var(--color-surface-hover)] text-[var(--color-text-muted)]",
  cyan: "bg-[color-mix(in_srgb,var(--color-admin-cyan)_16%,transparent)] text-[var(--color-admin-cyan)]",
  teal: "bg-[color-mix(in_srgb,var(--color-admin-teal)_16%,transparent)] text-[var(--color-admin-teal)]",
  success:
    "bg-[color-mix(in_srgb,var(--color-success)_16%,transparent)] text-[var(--color-success-text)]",
  warn: "bg-[color-mix(in_srgb,var(--color-admin-warn)_18%,transparent)] text-[var(--color-admin-warn)]",
  negative:
    "bg-[color-mix(in_srgb,var(--color-admin-negative)_16%,transparent)] text-[var(--color-admin-negative)]",
  // These two carry their own background token rather than a color-mix: the mockups specify the
  // fill alpha directly and it differs between light and dark (globals.css, `--color-accent-*-bg`).
  blue: "bg-[var(--color-accent-blue-bg)] text-[var(--color-accent-blue)]",
  purple: "bg-[var(--color-accent-purple-bg)] text-[var(--color-accent-purple)]",
} as const;

export type BadgeVariant = keyof typeof VARIANT;

type BadgeProps = ComponentPropsWithoutRef<"span"> & {
  variant?: BadgeVariant;
};

export function Badge({ variant = "neutral", className, children, ...props }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-semibold whitespace-nowrap",
        VARIANT[variant],
        className,
      )}
      {...props}
    >
      {children}
    </span>
  );
}
