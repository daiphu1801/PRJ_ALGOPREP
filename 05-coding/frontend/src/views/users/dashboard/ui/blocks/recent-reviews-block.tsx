// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import Link from "next/link";
import { useRecentSolutionReviews, type RecentSolutionReview } from "../../api";
import { useT } from "@/shared/i18n";
import { DashboardBlockState } from "@/shared/ui";

/**
 * 09-layoutBase/Dashboard AlgoPrep.dc.html:315-340 — "Solution Review": three recent reports, each
 * a coloured dot + problem title + date + measured complexity, plus a "Tất cả" link.
 *
 * ai-review-owned (F5.1) like the interviews block, so it too fails on its own (REQ-07).
 */
const VERDICT_COLOR: Record<RecentSolutionReview["verdict"], string> = {
  optimal: "--color-success",
  improvable: "--color-admin-warn",
  suboptimal: "--color-danger",
};

export function RecentReviewsBlock() {
  const t = useT("dashboard");
  const query = useRecentSolutionReviews();
  const reviews = query.data ?? [];

  return (
    <DashboardBlockState
      title={t("reviews.title")}
      isLoading={query.isLoading}
      isError={query.isError}
      isEmpty={query.data !== undefined && reviews.length === 0}
      onRetry={() => query.refetch()}
      emptyMessage={t("reviews.empty")}
      errorMessage={t("errorAi")}
      retryLabel={t("retry")}
    >
      <div>
        <ul className="flex flex-col gap-3">
          {reviews.map((review) => (
            <li key={review.submissionId} className="flex items-start gap-2.5">
              <span
                aria-label={t(`reviews.verdict.${review.verdict}`)}
                className="mt-1 h-[7px] w-[7px] shrink-0 rounded-sm"
                style={{ background: `var(${VERDICT_COLOR[review.verdict]})` }}
              />
              <div className="min-w-0 flex-1">
                <div className="flex items-baseline justify-between gap-2">
                  <Link
                    href={`/submissions/${review.submissionId}/review`}
                    className="truncate text-[13.5px] font-medium hover:underline"
                  >
                    {review.problemTitle}
                  </Link>
                  <span className="shrink-0 font-mono text-xs text-[var(--color-text-subtle)]">
                    {review.reviewedAt}
                  </span>
                </div>
                <p className="mt-0.5 font-mono text-xs text-[var(--color-text-muted)]">
                  {review.timeComplexity}
                </p>
              </div>
            </li>
          ))}
        </ul>
        <Link href="/submissions" className="mt-2.5 inline-block text-[12.5px] font-semibold underline">
          {t("seeAll")}
        </Link>
      </div>
    </DashboardBlockState>
  );
}
