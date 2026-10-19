// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Short "recent items" list for overview cards: an optional leading mark, a title with one line of context, and
// an optional trailing slot (a status badge, a time). A row can be a link. Generic (no domain type), so the
// instructor overview uses it for recent submissions and recent activity and other overviews can reuse it.
import type { ReactNode } from "react";
import Link from "next/link";
import { cn } from "@/shared/lib";

export type RecentListItem = {
  id: string;
  leading?: ReactNode;
  title: ReactNode;
  subtitle?: ReactNode;
  trailing?: ReactNode;
  /** When set the whole row is a link. */
  href?: string;
};

export function RecentList({
  items,
  emptyLabel,
  className,
}: {
  items: RecentListItem[];
  emptyLabel: string;
  className?: string;
}) {
  if (items.length === 0) {
    return (
      <p className="text-[13px] text-[var(--color-text-muted)]">{emptyLabel}</p>
    );
  }
  return (
    <ul className={cn("flex flex-col", className)}>
      {items.map((item) => {
        const content = (
          <>
            {item.leading ? (
              <span className="shrink-0">{item.leading}</span>
            ) : null}
            <span className="min-w-0 flex-1">
              <span className="block truncate text-[13px] font-medium">
                {item.title}
              </span>
              {item.subtitle ? (
                <span className="block truncate text-[11.5px] text-[var(--color-text-muted)]">
                  {item.subtitle}
                </span>
              ) : null}
            </span>
            {item.trailing ? (
              <span className="shrink-0">{item.trailing}</span>
            ) : null}
          </>
        );
        const rowClass =
          "flex items-center gap-2.5 border-b border-[var(--color-border)] py-2 last:border-b-0";
        return (
          <li key={item.id}>
            {item.href ? (
              <Link
                href={item.href}
                className={cn(
                  rowClass,
                  "rounded-md hover:bg-[var(--color-surface-hover)]",
                )}
              >
                {content}
              </Link>
            ) : (
              <div className={rowClass}>{content}</div>
            )}
          </li>
        );
      })}
    </ul>
  );
}
