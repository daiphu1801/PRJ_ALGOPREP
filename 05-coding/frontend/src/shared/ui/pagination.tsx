// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Page stepper for the Admin tables. 6 of the 13 Admin/shared BD files specify phân trang
// (02-bd/screens/admin/admin_user_management.md, admin_system_log.md, admin_ai_usage.md,
// shared/problem_management.md, interview_question_management.md, problem_authoring.md).
//
// All user-visible strings arrive as props: shared/ui must not reach into i18n, or the primitive
// stops being presentational (01-rd/system/frontend_architecture.md section 2.A).
"use client";

import { cn } from "@/shared/lib";
import { Button } from "./button";

type PaginationProps = {
  page: number;
  pageSize: number;
  total: number;
  onPageChange: (page: number) => void;
  /** e.g. "1-20 trên 143" — the caller formats it, since number formatting is locale-dependent. */
  summary: string;
  previousLabel: string;
  nextLabel: string;
  /**
   * Renders numbered page buttons between the two arrows
   * (09-layoutBase/Admin - Quản lý bài tập.dc.html:241-246). Off by default: a timeline-style list
   * jumps by "load more" instead, and numbers there would promise a fixed pagination that is not
   * how that data reads.
   */
  showPageNumbers?: boolean;
  /** Accessible label for one page button, e.g. `Trang ${n}`. Required with showPageNumbers. */
  pageLabel?: (page: number) => string;
  className?: string;
};

export function Pagination({
  page,
  pageSize,
  total,
  onPageChange,
  summary,
  previousLabel,
  nextLabel,
  showPageNumbers = false,
  pageLabel,
  className,
}: PaginationProps) {
  const lastPage = Math.max(1, Math.ceil(total / pageSize));
  // A sliding window of five, clamped at both ends, so a 143-page list does not render 143 buttons.
  const windowStart = Math.max(1, Math.min(page - 2, lastPage - 4));
  const pages = Array.from(
    { length: Math.min(5, lastPage) },
    (_, index) => windowStart + index,
  );

  return (
    <nav
      aria-label={summary}
      className={cn("flex items-center justify-between gap-3 pt-3 text-[13px]", className)}
    >
      <span className="text-[var(--color-text-muted)]">{summary}</span>
      <span className="flex items-center gap-1.5">
        <Button
          variant="ghost"
          size="sm"
          disabled={page <= 1}
          onClick={() => onPageChange(page - 1)}
        >
          {previousLabel}
        </Button>
        {showPageNumbers && pageLabel
          ? pages.map((candidate) => (
              <Button
                key={candidate}
                variant="ghost"
                size="sm"
                aria-current={candidate === page ? "page" : undefined}
                aria-label={pageLabel(candidate)}
                onClick={() => onPageChange(candidate)}
                className={
                  candidate === page
                    ? "border border-[var(--admin-active-border)] bg-[image:var(--color-row-selected)] font-mono"
                    : "font-mono"
                }
              >
                {candidate}
              </Button>
            ))
          : null}
        <Button
          variant="ghost"
          size="sm"
          disabled={page >= lastPage}
          onClick={() => onPageChange(page + 1)}
        >
          {nextLabel}
        </Button>
      </span>
    </nav>
  );
}
