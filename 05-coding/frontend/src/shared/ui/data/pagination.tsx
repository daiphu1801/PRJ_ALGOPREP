// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Page stepper for the Admin tables. 6 of the 13 Admin/shared BD files specify phân trang
// (02-bd/screens/admin/admin_user_management.md, admin_system_log.md, admin_ai_usage.md,
// shared/problem_management.md, interview_question_management.md, problem_authoring.md).
//
// All user-visible strings arrive as props: shared/ui must not reach into i18n, or the primitive
// stops being presentational (01-rd/system/SYS0102_frontend_architecture.md section 2.A).
"use client";

import { cn } from "@/shared/lib";
import { Button } from "../primitives/button";
import { FilterMenu } from "./filter-menu";

type PaginationProps = {
  page: number;
  pageSize: number;
  /** Total matching rows, when the API provides it. */
  total?: number;
  /** Whether another page exists when the API uses a cursor. */
  hasNextPage?: boolean;
  nextPending?: boolean;
  onPageChange: (page: number) => void;
  /** e.g. "1-20 trên 143" — the caller formats it, since number formatting is locale-dependent. */
  summary: string;
  previousLabel: string;
  nextLabel: string;
  /**
   * Renders numbered page buttons between the two arrows
   * (09-layoutBase/Admin - Quản lý bài tập.dc.html:241-246). Requires a known total.
   */
  showPageNumbers?: boolean;
  /** Accessible label for one page button, e.g. `Trang ${n}`. Required with showPageNumbers. */
  pageLabel?: (page: number) => string;
  /** Rows-per-page choices; with `onPageSizeChange` renders a picker before the arrows. */
  pageSizeOptions?: readonly number[];
  onPageSizeChange?: (size: number) => void;
  /** Accessible name of the picker, e.g. "Số dòng mỗi trang". */
  pageSizeLabel?: string;
  /** Render nothing while every row fits on one page (the stepper would only say "1 / 1"). */
  hideWhenSinglePage?: boolean;
  className?: string;
};

export function Pagination({
  page,
  pageSize,
  total,
  hasNextPage,
  nextPending = false,
  onPageChange,
  summary,
  previousLabel,
  nextLabel,
  showPageNumbers = false,
  pageLabel,
  pageSizeOptions,
  onPageSizeChange,
  pageSizeLabel,
  hideWhenSinglePage = false,
  className,
}: PaginationProps) {
  const lastPage = Math.max(1, Math.ceil((total ?? 0) / pageSize));
  if (hideWhenSinglePage && total !== undefined && lastPage === 1) return null;
  // A sliding window of five, clamped at both ends, so a 143-page list does not render 143 buttons.
  const windowStart = Math.max(1, Math.min(page - 2, lastPage - 4));
  const pages = Array.from(
    { length: Math.min(5, lastPage) },
    (_, index) => windowStart + index,
  );

  return (
    <nav
      aria-label={summary}
      className={cn(
        "flex flex-wrap items-center justify-between gap-3 pt-3 text-[13px]",
        className,
      )}
    >
      <span className="text-[var(--color-text-muted)]">{summary}</span>
      <span className="flex flex-wrap items-center gap-1.5">
        {pageSizeOptions && onPageSizeChange ? (
          // Same glass pop-up as the list filters; opens upward because this bar sits at the card's foot.
          <FilterMenu
            compact
            placement="top"
            label={pageSizeLabel ?? ""}
            options={pageSizeOptions.map((option) => ({
              value: String(option),
              label: String(option),
            }))}
            value={String(pageSize)}
            onValueChange={(next) => onPageSizeChange(Number(next))}
            className="mr-1.5"
          />
        ) : null}
        <Button
          variant="ghost"
          size="sm"
          disabled={page <= 1}
          onClick={() => onPageChange(page - 1)}
        >
          {previousLabel}
        </Button>
        {total !== undefined && showPageNumbers && pageLabel
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
          disabled={nextPending || !(hasNextPage ?? page < lastPage)}
          onClick={() => onPageChange(page + 1)}
        >
          {nextLabel}
        </Button>
      </span>
    </nav>
  );
}
