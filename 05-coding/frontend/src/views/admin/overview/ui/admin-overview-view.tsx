// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Activity on the left and distribution and rankings on the right when space allows.
//
// No VISIBLE page heading: 09-layoutBase/Admin - Tổng quan.dc.html:103-122 goes straight from the
// toolbar into the grid rows, no "Tổng quan" title anywhere on the page — matching the prototype
// 1:1 per BD section 2 ("Dựa 1:1 theo cấu trúc HTML của prototype"). An `sr-only` h1 is still kept
// below so screen-reader users get a page landmark even though the static mock never needed one.
import { getT } from "@/shared/i18n/server";
import { StatCardsRow } from "./blocks/stat-cards-row";
import { SubmissionsByDayBlock } from "./blocks/submissions-by-day-block";
import { SubmissionsByLanguageBlock } from "./blocks/submissions-by-language-block";
import { SubmissionsByMonthBlock } from "./blocks/submissions-by-month-block";
import { TopProblemsBlock } from "./blocks/top-problems-block";
import { VerdictDistributionBlock } from "./blocks/verdict-distribution-block";

export async function AdminOverviewView() {
  const t = await getT("adminOverview");

  return (
    <div className="@container flex min-w-0 flex-1 flex-col gap-3">
      <h1 className="sr-only">{t("title")}</h1>

      <div className="grid grid-cols-1 gap-3 @min-[880px]:grow-[2] @min-[880px]:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
        <div className="flex min-w-0 flex-col gap-3">
          <StatCardsRow />
          <div className="@min-[880px]:relative @min-[880px]:min-h-[clamp(12rem,calc(calc(55*var(--dvh1))-13rem),18.75rem)] @min-[880px]:flex-1">
            <SubmissionsByDayBlock />
          </div>
        </div>
        <div className="flex min-w-0 flex-col gap-3">
          <VerdictDistributionBlock />
          <TopProblemsBlock />
        </div>
      </div>
      <div className="grid grid-cols-1 gap-3 @min-[880px]:grow @min-[880px]:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
        <SubmissionsByMonthBlock />
        <SubmissionsByLanguageBlock />
      </div>
    </div>
  );
}
