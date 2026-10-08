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
// Still not built, both deliberate: "Xem lộ trình" (RD mục 4 Q1 — no Fx-nn describes a roadmap
// feature) and the prototype's per-criterion rubric bars (the DTO has no such field). Ledger
// section 12.
"use client";

import { useState } from "react";
import type { ProgressRange } from "@/entities/progress";
import { useT } from "@/shared/i18n";
import { SegmentedTabs } from "@/shared/ui";
import { ActivityBlock } from "./blocks/activity-block";
import { DailySubmissionsBlock } from "./blocks/daily-submissions-block";
import { DifficultyCard } from "./blocks/difficulty-card";
import { FocusCard } from "./blocks/focus-card";
import { GreetingBlock } from "./blocks/greeting-block";
import { RecentInterviewsBlock } from "./blocks/recent-interviews-block";
import { RecentReviewsBlock } from "./blocks/recent-reviews-block";
import { SkillRadarBlock } from "./blocks/skill-radar-block";
import { StatCardsRow } from "./blocks/stat-cards-row";
import { SuggestedProblemsBlock } from "./blocks/suggested-problems-block";
import { TopicsTable } from "./blocks/topics-table";

/** Both halves of the merged screen use the same 1.55fr/1fr split the two prototypes shared. */
const TWO_COLUMN =
  "grid grid-cols-1 items-start gap-3.5 lg:grid-cols-[1.55fr_1fr]";

export function DashboardView() {
  const t = useT("dashboard");
  const [range, setRange] = useState<ProgressRange>("30d");

  return (
    <div className="py-6">
      <h1 className="sr-only">{t("title")}</h1>

      <div className="mb-5 flex flex-wrap items-end justify-between gap-4">
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

      <StatCardsRow />

      <div className={`${TWO_COLUMN} mb-3.5`}>
        <DailySubmissionsBlock range={range} />
        <SkillRadarBlock />
      </div>

      <div className="mb-3.5">
        <ActivityBlock />
      </div>

      {/* The merged my_progress half: the full topic table replaces the dashboard prototype's
          shortened progress bars, which said strictly less than the table does. */}
      <div className={`${TWO_COLUMN} mb-3.5`}>
        <TopicsTable range={range} />
        <div className="flex flex-col gap-3.5">
          <DifficultyCard />
          <FocusCard range={range} />
        </div>
      </div>

      <div className={TWO_COLUMN}>
        <SuggestedProblemsBlock />
        <div className="flex flex-col gap-3.5">
          <RecentInterviewsBlock />
          <RecentReviewsBlock />
        </div>
      </div>
    </div>
  );
}
