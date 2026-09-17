// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Layout follows 09-layoutBase/Admin - Token AI.dc.html: sticky header with a range switch and an
// export CTA (:147-162), stat cards (:164-176), a 1.6fr / minmax(300px,1fr) grid holding the daily
// chart and the budget/alert column (:178-244), then two ranked tables side by side (:246-300).
//
// The cut "Gợi ý theo bậc" feature (DEC-2026-0831-remove-tiered-hints-ai-config) is stripped in
// three places the mockup still carries it: the chart's fourth series, the "Gợi ý" legend entry,
// and the top-problems column that counted "lượt gợi ý". Three AI features remain —
// BD section 7 ("Hiện đủ 3 tính năng còn lại").
//
// Two documented behaviours worth not getting wrong:
// - The anomalous-account notice is a SOFT warning for an admin to review, never an automatic lock
//   (DEC-2026-0831-ai-usage-anomaly-alert). It renders as a notice with no action attached.
// - The budget panel stays read-only; unlocking belongs to admin_ai_config, so two screens do not
//   both write `ai_token_budget_configs` (BD section 7).
//
// Known gap: the "Sinh testcase" feature has no owning module. `prompt_templates.feature_code` has
// no GENERATE_TESTCASE — level B2 in 07-review/bd_screens_admin_open_questions_260913.md.
"use client";

import { useState } from "react";
import {
  fetchAiUsagePage,
  type AiFeature,
  type TopProblem,
  type TopUser,
  type UsageRange,
} from "@/entities/ai-usage";
import { useT } from "@/shared/i18n";
import {
  Badge,
  Button,
  Card,
  DataTable,
  NoticeTile,
  PageHeader,
  ProgressBar,
  RankedProgressList,
  SegmentedTabs,
  StackedBarChart,
  StatCard,
  type BadgeVariant,
  type DataTableColumn,
} from "@/shared/ui";

// dc.html:477-480 colours the series #0EA5A4 / #64C8C0 / amber. Mapped onto tokens the project
// already has rather than forking near-identical teals.
const FEATURE_COLOR_VAR: Record<AiFeature, string> = {
  review: "--color-admin-teal",
  interview: "--color-admin-cyan",
  testcase: "--color-admin-warn",
};

const DIFFICULTY_VARIANT: Record<TopProblem["difficulty"], BadgeVariant> = {
  easy: "success",
  medium: "warn",
  hard: "negative",
};

export function AdminAiUsageView() {
  const t = useT("adminAiUsage");
  const [page] = useState(fetchAiUsagePage);
  const [range, setRange] = useState<UsageRange>("14d");

  const days = page.daily[range];

  const userColumns: DataTableColumn<TopUser>[] = [
    {
      key: "rank",
      header: "#",
      width: "28px",
      render: (user) => (
        <span className="font-mono text-[11.5px] text-[var(--color-text-subtle)]">{user.rank}</span>
      ),
    },
    {
      key: "user",
      header: t("columnLearner"),
      render: (user) => (
        <span className="block min-w-0">
          <span className="block truncate font-semibold">{user.name}</span>
          <span className="block truncate text-[11.5px] text-[var(--color-text-subtle)]">
            {user.meta}
          </span>
        </span>
      ),
    },
    {
      key: "tokens",
      header: t("columnTokens"),
      width: "84px",
      align: "right",
      render: (user) => <span className="font-mono font-semibold">{user.tokens}</span>,
    },
    {
      key: "calls",
      header: t("columnCalls"),
      width: "74px",
      align: "right",
      render: (user) => (
        <span className="font-mono text-[var(--color-text-muted)]">{user.calls}</span>
      ),
    },
    {
      key: "cost",
      header: t("columnCost"),
      width: "76px",
      align: "right",
      render: (user) => (
        <span className="font-mono text-[var(--color-text-muted)]">{user.cost}</span>
      ),
    },
  ];

  const problemColumns: DataTableColumn<TopProblem>[] = [
    {
      key: "rank",
      header: "#",
      width: "28px",
      render: (problem) => (
        <span className="font-mono text-[11.5px] text-[var(--color-text-subtle)]">
          {problem.rank}
        </span>
      ),
    },
    {
      key: "problem",
      header: t("columnProblem"),
      render: (problem) => (
        <span className="block min-w-0">
          <span className="block truncate font-semibold">{problem.name}</span>
          <span className="mt-0.5 flex items-center gap-2 text-[11.5px] text-[var(--color-text-subtle)]">
            <Badge variant={DIFFICULTY_VARIANT[problem.difficulty]}>
              {t(`difficulty.${problem.difficulty}`)}
            </Badge>
            {t("callCount", { count: problem.calls })}
          </span>
        </span>
      ),
    },
    {
      key: "tokens",
      header: t("columnTokens"),
      width: "84px",
      align: "right",
      render: (problem) => <span className="font-mono font-semibold">{problem.tokens}</span>,
    },
    {
      key: "average",
      header: t("columnAveragePerCall"),
      width: "92px",
      align: "right",
      render: (problem) => (
        <span className="font-mono text-[var(--color-text-muted)]">{problem.averagePerCall}</span>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        actions={
          <>
            <SegmentedTabs
              label={t("rangeLabel")}
              value={range}
              onValueChange={setRange}
              options={[
                { value: "7d", label: t("range.7d") },
                { value: "14d", label: t("range.14d") },
                { value: "30d", label: t("range.30d") },
              ]}
            />
            <Button variant="cta" size="sm">
              {t("export")}
            </Button>
          </>
        }
      />

      <div className="mb-4 grid gap-3.5 [grid-template-columns:repeat(auto-fit,minmax(210px,1fr))]">
        {page.stats.map((stat) => (
          <StatCard
            key={stat.key}
            label={t(`stat.${stat.key}.label`)}
            value={stat.value}
            delta={<span style={{ color: `var(${stat.deltaColorVar})` }}>{stat.delta}</span>}
            meta={t(`stat.${stat.key}.meta`)}
          />
        ))}
      </div>

      <div className="mb-4 grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,1.6fr)_minmax(300px,1fr)]">
        <Card
          title={t("dailyChartTitle")}
          description={t("dailyChartSubtitle", { days: days.length })}
          className="min-w-0"
        >
          <StackedBarChart
            series={[
              { key: "review", label: t("feature.review"), colorVar: FEATURE_COLOR_VAR.review },
              { key: "interview", label: t("feature.interview"), colorVar: FEATURE_COLOR_VAR.interview },
              { key: "testcase", label: t("feature.testcase"), colorVar: FEATURE_COLOR_VAR.testcase },
            ]}
            bars={days.map((day) => ({
              label: day.label,
              title: day.label,
              values: day.values,
            }))}
          />
        </Card>

        <div className="flex min-w-0 flex-col gap-4">
          <Card
            title={t("budgetTitle")}
            description={t("budgetSubtitle", {
              used: page.budget.usedLabel,
              cap: page.budget.capLabel,
            })}
          >
            <ProgressBar
              value={page.budget.percent}
              label={t("budgetTitle")}
              fill="linear-gradient(90deg, var(--color-admin-teal), var(--color-admin-warn))"
            />
            <p className="mt-2 mb-4 flex justify-between text-[12.5px] text-[var(--color-text-muted)]">
              <span>{t("budgetLeft", { left: page.budget.leftLabel })}</span>
              <span>{t("budgetRunOut", { date: page.budget.runOutLabel })}</span>
            </p>
            <RankedProgressList
              numbered={false}
              items={page.featureShares.map((share) => ({
                label: t(`feature.${share.feature}`),
                value: share.percent,
                valueLabel: share.label,
                colorVar: FEATURE_COLOR_VAR[share.feature],
              }))}
            />
          </Card>

          <Card title={t("alertsTitle")}>
            <div className="flex flex-col gap-2.5">
              {page.alerts.map((alert) => (
                <NoticeTile
                  key={alert.key}
                  tone={alert.tone}
                  title={t(`alert.${alert.key}.title`)}
                >
                  {t(`alert.${alert.key}.meta`)}
                </NoticeTile>
              ))}
            </div>
            <p className="mt-3.5 text-[12.5px] text-[var(--color-text-subtle)]">
              {t("alertsFootnote")}
            </p>
          </Card>
        </div>
      </div>

      <div className="grid gap-4 [grid-template-columns:repeat(auto-fit,minmax(380px,1fr))]">
        <Card
          title={t("topUsersTitle")}
          description={t("topUsersSubtitle")}
          className="min-w-0 px-[18px]"
        >
          <DataTable
            caption={t("topUsersTitle")}
            columns={userColumns}
            rows={page.topUsers}
            rowKey={(user) => user.meta}
            emptyMessage={t("emptyUsers")}
            minWidth={420}
          />
        </Card>

        <Card
          title={t("topProblemsTitle")}
          description={t("topProblemsSubtitle")}
          className="min-w-0 px-[18px]"
        >
          <DataTable
            caption={t("topProblemsTitle")}
            columns={problemColumns}
            rows={page.topProblems}
            rowKey={(problem) => problem.name}
            emptyMessage={t("emptyProblems")}
            minWidth={420}
          />
        </Card>
      </div>
    </div>
  );
}
