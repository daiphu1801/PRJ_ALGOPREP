// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Full layout per 02-bd/screens/admin/admin_overview.md section 2: 3 grid rows, 9 independently
// stateful data blocks. The sidebar/toolbar shell itself lives in widgets/app-shell (extended for
// area="admin", see the impact note in 06-plan/reports/ for this run) because BD section 2 point 1
// says the shell is shared across all 11 Admin nav destinations, not specific to this screen.
//
// No VISIBLE page heading: 09-layoutBase/Admin - Tổng quan.dc.html:103-122 goes straight from the
// toolbar into the grid rows, no "Tổng quan" title anywhere on the page — matching the prototype
// 1:1 per BD section 2 ("Dựa 1:1 theo cấu trúc HTML của prototype"). An `sr-only` h1 is still kept
// below so screen-reader users get a page landmark even though the static mock never needed one.
import { getT } from "@/shared/i18n/server";
import { DifficultyBreakdownBlock } from "./blocks/difficulty-breakdown-block";
import { StatCardsRow } from "./blocks/stat-cards-row";
import { SubmissionsByDayBlock } from "./blocks/submissions-by-day-block";
import { SubmissionsByLanguageBlock } from "./blocks/submissions-by-language-block";
import { SubmissionsByMonthBlock } from "./blocks/submissions-by-month-block";
import { TopProblemsBlock } from "./blocks/top-problems-block";
import { UserRetentionBlock } from "./blocks/user-retention-block";
import { VerdictDistributionBlock } from "./blocks/verdict-distribution-block";

export async function AdminOverviewView() {
  const t = await getT("adminOverview");

  return (
    <div className="flex flex-col gap-3">
      <h1 className="sr-only">{t("title")}</h1>

      {/* Row 1: 1fr 1.25fr 1fr (02-bd/screens/admin/admin_overview.md section 2 point 3) */}
      <div className="grid grid-cols-1 gap-3 lg:grid-cols-[1fr_1.25fr_1fr]">
        <StatCardsRow />
        <SubmissionsByLanguageBlock />
        <VerdictDistributionBlock />
      </div>

      {/* Row 2: 1.3fr 1fr (section 2 point 4) */}
      <div className="grid grid-cols-1 gap-3 lg:grid-cols-[1.3fr_1fr]">
        <DifficultyBreakdownBlock />
        <SubmissionsByDayBlock />
      </div>

      {/* Row 3: 1fr 1fr 1fr (section 2 point 5) */}
      <div className="grid grid-cols-1 gap-3 lg:grid-cols-3">
        <SubmissionsByMonthBlock />
        <TopProblemsBlock />
        <UserRetentionBlock />
      </div>
    </div>
  );
}
