// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 12.
//
// USR0601_dashboard — the single overview screen of the Student area. Two decisions built it:
// `DEC-2026-0927-student-dashboard-home` created it and made it the post-login destination, then
// `DEC-2026-0927-student-area-merge-and-shared-shell` MERGED `my_progress` (USR0501) into it, so
// `/progress` no longer exists. That merge is why the three blocks below carry
// `views/my-progress` lineage — TopicsTable, DifficultyCard and FocusCard moved here whole rather
// than being rewritten, which is also why they still read the `myProgress` message namespace.
//
// Layout follows 09-layoutBase/Dashboard AlgoPrep.dc.html for the dashboard half (greeting :99-108,
// stat cards :110-125, daily chart + radar :127-185, activity grid :187-213, suggested problems +
// side column :215-340) and 09-layoutBase/Tiến độ của tôi.dc.html for the merged half (topics
// table :115-149, difficulty :166-181, focus :183-196).
//
// ONE range control for the whole screen, on the greeting row — the merged `my_progress` had a
// screen-wide range and the dashboard had a chart-local one, and keeping both would have put two
// controls with different vocabularies on one page. This is why the view is a Client Component,
// unlike admin_overview which is a server shell over client blocks.
//
// Still not built: "Xem lộ trình" (dropped for good, RD mục 4 Q1 — no Fx-nn describes a roadmap
// feature). Per-topic interview-question completion (F6-10, BD Khu vực F NO 10-14) is InterviewTopicsTable (F6 #8, no range). The range control also drives the interview block.
// Ledger section 12.
"use client";

import { useState } from "react";
import type { ProgressRange } from "@/entities/progress";
import { useT } from "@/shared/i18n";
import { SegmentedTabs } from "@/shared/ui";
import { DailySubmissionsBlock } from "./blocks/daily-submissions-block";
import { DifficultyCard } from "./blocks/difficulty-card";
import { GreetingBlock } from "./blocks/greeting-block";
import { InterviewTopicsTable } from "./blocks/interview-topics-table";
import { RecentInterviewsBlock } from "./blocks/recent-interviews-block";
import { RecentReviewsBlock } from "./blocks/recent-reviews-block";
import { StatCardsRow } from "./blocks/stat-cards-row";
import { SuggestedProblemsBlock } from "./blocks/suggested-problems-block";
import { TopicsTable } from "./blocks/topics-table";

/**
 * Two columns of about the same height, not rows of paired cards: the blocks have very different heights (165 to
 * 400px), and pairing them left a blank stretch under the shorter one (owner 2026-11-23). The LAST card of each column
 * grows, so both columns end on the same line; on a narrow screen they simply stack.
 */
const COLUMNS = "grid grid-cols-1 items-stretch gap-3.5 lg:grid-cols-2";
const COLUMN = "flex min-w-0 flex-col gap-3.5 [&>:last-child]:grow";

const RANGE_LABEL_KEY = {
  "7d": "range7d",
  "30d": "range30d",
  all: "rangeAll",
} as const;

export function DashboardView() {
  const t = useT("dashboard");
  const [range, setRange] = useState<ProgressRange>("30d");

  return (
    <div>
      <h1 className="sr-only">{t("title")}</h1>

      <div className="mb-3 flex flex-wrap items-end justify-between gap-4">
        <GreetingBlock />
        <SegmentedTabs
          label={t("rangeLabel")}
          value={range}
          onValueChange={setRange}
          options={[
            { value: "7d", label: t("range7d") },
            { value: "30d", label: t("range30d") },
            { value: "all", label: t("rangeAll") },
          ]}
        />
      </div>

      {/* Changing the range recomputes the range-aware blocks silently; this tells a screen reader which range now applies. */}
      <p role="status" className="sr-only">
        {t("rangeAnnounce", { range: t(RANGE_LABEL_KEY[range]) })}
      </p>

      <StatCardsRow />

      {/* Trimmed 2026-11-23 (DEC-2026-1123-student-dashboard-trim): the radar and "Nên ưu tiên" repeated what the
          topic table and the greeting already say (and could name a different weakest topic), and the 12-month
          heatmap repeated the daily chart. What is left, each shown once, in two balanced columns. */}
      <div className={`${COLUMNS} mb-3.5`}>
        <div className={COLUMN}>
          <DailySubmissionsBlock range={range} />
          <TopicsTable range={range} />
          <SuggestedProblemsBlock />
        </div>
        <div className={COLUMN}>
          <DifficultyCard />
          <InterviewTopicsTable />
          <RecentInterviewsBlock range={range} />
        </div>
      </div>

      <RecentReviewsBlock />
    </div>
  );
}
