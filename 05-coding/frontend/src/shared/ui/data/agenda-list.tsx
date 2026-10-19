// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Entries grouped by day: a day label on the left, its entries stacked on the right, each with a time, a title
// and one line of context. Days without entries are not drawn. Generic (no domain type), so any overview that
// needs a "this week" agenda (the instructor teaching schedule today) can reuse it.
import type { ReactNode } from "react";
import { cn } from "@/shared/lib";

export type AgendaEntry = {
  id: string;
  time: string;
  title: ReactNode;
  meta?: ReactNode;
  /** CSS custom-property name colouring the entry's left bar. */
  colorVar?: string;
};

export type AgendaDay = {
  key: string;
  label: string;
  entries: AgendaEntry[];
  /** Marks the day the viewer is on (today). */
  highlighted?: boolean;
};

export function AgendaList({
  days,
  emptyLabel,
  className,
}: {
  days: AgendaDay[];
  emptyLabel: string;
  className?: string;
}) {
  const filled = days.filter((day) => day.entries.length > 0);
  if (filled.length === 0) {
    return (
      <p className="text-[13px] text-[var(--color-text-muted)]">{emptyLabel}</p>
    );
  }
  return (
    <ul className={cn("flex flex-col gap-3", className)}>
      {filled.map((day) => (
        <li key={day.key} className="flex items-start gap-3">
          <span
            className={cn(
              "mt-0.5 w-[52px] shrink-0 rounded-md px-1.5 py-0.5 text-center text-[11.5px] font-semibold",
              day.highlighted
                ? "bg-[var(--color-primary)] text-white"
                : "text-[var(--color-text-muted)]",
            )}
            aria-current={day.highlighted ? "date" : undefined}
          >
            {day.label}
          </span>
          <ul className="flex min-w-0 flex-1 flex-col gap-2">
            {day.entries.map((entry) => (
              <li
                key={entry.id}
                className="flex items-stretch gap-2.5 text-[13px]"
              >
                <span
                  aria-hidden="true"
                  className="w-[3px] shrink-0 rounded-full"
                  style={{
                    background: `var(${entry.colorVar ?? "--color-primary"})`,
                  }}
                />
                <div className="min-w-0">
                  <p className="font-semibold">
                    <span className="mr-2 font-mono text-[12px] font-normal text-[var(--color-text-muted)]">
                      {entry.time}
                    </span>
                    {entry.title}
                  </p>
                  {entry.meta ? (
                    <p className="truncate text-[11.5px] text-[var(--color-text-muted)]">
                      {entry.meta}
                    </p>
                  ) : null}
                </div>
              </li>
            ))}
          </ul>
        </li>
      ))}
    </ul>
  );
}
