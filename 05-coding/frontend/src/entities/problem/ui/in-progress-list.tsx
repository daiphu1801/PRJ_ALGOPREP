// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// "In-progress problems" cards shared by USR0101 (right column, 3 cards plus a "view all" link) and
// USR0105 (workspace home, up to 12 cards in a grid). Each card resumes the problem in the Workspace.
"use client";

import Link from "next/link";
import { SeeAllLink } from "@/shared/ui";
import { useT } from "@/shared/i18n";
import type { InProgressProblem } from "../model/types";

type Props = {
  items: InProgressProblem[];
  /** Max cards shown. */
  limit: number;
  /** "compact" = single column (side panel), "grid" = responsive grid (full page). */
  layout: "compact" | "grid";
  /** Text shown when there is nothing to list. */
  emptyText: string;
  /** Optional "view all" link rendered under the list. */
  footerLink?: { href: string; label: string };
};

export function InProgressList({
  items,
  limit,
  layout,
  emptyText,
  footerLink,
}: Props) {
  const tl = useT("problemList");
  const td = useT("problemDetail");
  const shown = items.slice(0, limit);
  const compact = layout === "compact";

  if (shown.length === 0) {
    return (
      <p className="text-[12.5px] text-[var(--color-text-muted)]">{emptyText}</p>
    );
  }

  return (
    <>
      <ul
        className={
          compact
            ? "flex flex-col gap-2"
            : "grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3"
        }
      >
        {shown.map((item) => (
          <li key={item.problemId}>
            <Link
              href={`/workspace/${item.problemId}`}
              className={`block rounded-lg border border-[var(--color-border)] hover:bg-[var(--color-row-hover)] ${
                compact ? "px-2.5 py-2" : "px-3 py-2.5"
              }`}
            >
              <p
                className={`truncate font-semibold ${
                  compact ? "text-[12.5px]" : "text-[13px]"
                }`}
              >
                {item.title}
              </p>
              <p
                className={`text-[var(--color-text-muted)] ${
                  compact ? "text-[11.5px]" : "text-[12px]"
                }`}
              >
                {td(`language.${item.language}`)} ·{" "}
                {tl("inProgress.lastVerdict", {
                  verdict: td(`verdict.${item.lastVerdict}`),
                  passed: item.passedCount,
                  total: item.totalCount,
                })}
              </p>
            </Link>
          </li>
        ))}
      </ul>
      {footerLink ? (
        <SeeAllLink href={footerLink.href}>{footerLink.label}</SeeAllLink>
      ) : null}
    </>
  );
}
