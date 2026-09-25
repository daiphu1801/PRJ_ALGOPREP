// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Layout per 09-layoutBase/Tiến độ của tôi.dc.html: stats bar + range tabs (:97-108), two-column
// grid (:111-213), topics table (:115-149), submissions chart (:151-162), difficulty card
// (:166-181), focus card (:183-196), mock interview card (:198-211).
//
// USR0501_my_progress, layout per 02-bd/screens/users/USR0501_my_progress.md Sheet 4.4 (2-column,
// topics table + 14-day chart on the left, difficulty/focus/interview on the right). Client
// component (not server + client blocks like admin_overview) because the range filter is screen-
// wide local state that several blocks below read.
"use client";

import { useState } from "react";
import { useT } from "@/shared/i18n";
import type { ProgressRange } from "@/entities/progress";
import { DifficultyCard } from "./difficulty-card";
import { FocusCard } from "./focus-card";
import { MockInterviewCard } from "./mock-interview-card";
import { StatsBar } from "./stats-bar";
import { SubmissionsChart } from "./submissions-chart";
import { TopicsTable } from "./topics-table";

export function MyProgressView() {
  const t = useT("myProgress");
  const [range, setRange] = useState<ProgressRange>("30d");

  return (
    <div>
      <h1 className="sr-only">{t("title")}</h1>
      <StatsBar range={range} onRangeChange={setRange} />

      <div className="grid grid-cols-1 gap-3.5 lg:grid-cols-[minmax(0,1fr)_340px]">
        <div className="flex min-w-0 flex-col gap-3.5">
          <TopicsTable range={range} />
          <SubmissionsChart range={range} />
        </div>
        <div className="flex flex-col gap-3.5">
          <DifficultyCard />
          <FocusCard range={range} />
          <MockInterviewCard />
        </div>
      </div>
    </div>
  );
}
