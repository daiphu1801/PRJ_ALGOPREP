// One-line health summary above an operations screen: a coloured dot, the state in words, and an
// optional trailing slot (timestamp, link). Not a KPI card — it reports a single state, not a metric.
//
// The state is carried by the title text, the dot is decoration (aria-hidden), so colour is never the
// only signal. role="status" because it updates in place without interrupting.
import type { ReactNode } from "react";
import { cn } from "@/shared/lib";

const TONE = {
  success: "bg-[var(--color-success)]",
  warn: "bg-[var(--color-admin-warn)]",
  negative: "bg-[var(--color-admin-negative)]",
  neutral: "bg-[var(--color-text-subtle)]",
} as const;

export type StatusStripTone = keyof typeof TONE;

export function StatusStrip({
  tone,
  title,
  detail,
  trailing,
  className,
}: {
  tone: StatusStripTone;
  title: ReactNode;
  detail?: ReactNode;
  trailing?: ReactNode;
  className?: string;
}) {
  return (
    <div
      role="status"
      className={cn(
        "glass-surface mb-4 flex flex-wrap items-center gap-x-3 gap-y-1 rounded-2xl border border-[var(--color-border)] px-4 py-2.5",
        className,
      )}
    >
      <span
        aria-hidden="true"
        className={cn("h-2.5 w-2.5 shrink-0 rounded-full", TONE[tone])}
      />
      <span className="text-[13.5px] font-semibold">{title}</span>
      {detail ? (
        <span className="text-[12.5px] text-[var(--color-text-muted)]">
          {detail}
        </span>
      ) : null}
      {trailing ? (
        <span className="ml-auto text-[12.5px] text-[var(--color-text-muted)]">
          {trailing}
        </span>
      ) : null}
    </div>
  );
}
