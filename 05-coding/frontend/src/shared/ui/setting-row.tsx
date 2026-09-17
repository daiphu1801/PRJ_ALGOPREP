// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// One settings line: label + sub-description on the left, a value or control on the right.
// 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html builds seven of these on one screen with the
// same box (:210-217 read-only values, :226-234 with a toggle) — 11px/13px padding, 14px radius,
// glass-surface fill, 1px border.
//
// Takes only presentation props, so it works for a read-only figure, a toggle or an input alike.
import type { ReactNode } from "react";
import { cn } from "@/shared/lib";

type SettingRowProps = {
  label: ReactNode;
  description?: ReactNode;
  /** Leading element before the text, e.g. an avatar or an icon. */
  leading?: ReactNode;
  /** Value or control on the right. */
  children?: ReactNode;
  className?: string;
};

export function SettingRow({
  label,
  description,
  leading,
  children,
  className,
}: SettingRowProps) {
  return (
    <div
      className={cn(
        "glass-surface flex items-center gap-3 rounded-2xl border border-[var(--color-border)] px-3 py-[11px]",
        className,
      )}
    >
      {leading ? <div className="shrink-0">{leading}</div> : null}
      <div className="min-w-0 flex-1">
        <p className="text-[13.5px] font-semibold">{label}</p>
        {description ? (
          <p className="text-xs text-[var(--color-text-subtle)]">{description}</p>
        ) : null}
      </div>
      {children ? <div className="shrink-0">{children}</div> : null}
    </div>
  );
}
