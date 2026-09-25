// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Khu vực C — reuses shared/ui/charts/grouped-bar-chart.tsx with one bar per group (one day),
// legend omitted (this chart has a single series, unlike the two admin_overview charts that
// component was built for) rather than adding a near-duplicate "single bar chart" primitive.
"use client";

import { useT } from "@/shared/i18n";
import { Card, GroupedBarChart, Skeleton } from "@/shared/ui";
import { useDailySubmissions, type ProgressRange } from "@/entities/progress";

export function SubmissionsChart({ range }: { range: ProgressRange }) {
  const t = useT("myProgress");
  const query = useDailySubmissions(range);

  return (
    <Card title={t("chart.title")}>
      {query.isLoading || !query.data ? (
        <Skeleton className="h-[168px]" />
      ) : (
        <GroupedBarChart
          legend={[]}
          groups={query.data.map((point) => ({
            label: point.day,
            bars: [{ label: t("chart.tooltipCount"), value: point.count, colorVar: "--color-primary" }],
          }))}
        />
      )}
    </Card>
  );
}
