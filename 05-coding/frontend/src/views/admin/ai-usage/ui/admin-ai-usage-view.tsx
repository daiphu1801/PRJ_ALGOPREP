// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Layout follows 09-layoutBase/Admin - Token AI.dc.html: sticky header with a range switch (the
// mockup's export CTA was dropped 2026-10-10, no F-code behind it), stat cards (:164-176), a 1.6fr / minmax(300px,1fr) grid holding the daily
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
// - The budget panel stays read-only; unlocking belongs to admin_ai_limits (ADM0303), so two screens do not
//   both write `ai_token_budget_configs` (BD section 7). It reads the same `aiTokenBudget` store
//   ADM0303 writes, so a new cap or a lock shows here at once (BD ADM0302 Sheet 5 Khu vực D).
//
// "Sinh testcase" is feature_code GENERATE_TESTCASE (DEC-2026-1010-ai-generate-testcase-feature-code),
// so its prompt and token ledger live in the same tables as the other two features.
"use client";

import Link from "next/link";
import { useState } from "react";
import {
  useAiUsagePage,
  type AiFeature,
  type AiUsagePage,
  type TopProblem,
  type TopUser,
  type UsageAlert,
  type UsageRange,
} from "../api";
import {
  aiBudgetSettings,
  aiTokenBudget,
  useModelPrices,
} from "@/entities/ai-budget";
import {
  problemLevelLabel,
  problemLevelTone,
  useProblemLevels,
} from "@/entities/problem";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  DataTable,
  NoticeTile,
  PageHeader,
  QueryBoundary,
  ProgressBar,
  RankedProgressList,
  SegmentedTabs,
  StackedBarChart,
  type DataTableColumn,
} from "@/shared/ui";

// dc.html:477-480 colours the series #0EA5A4 / #64C8C0 / amber. Mapped onto tokens the project
// already has rather than forking near-identical teals.
const FEATURE_COLOR_VAR: Record<AiFeature, string> = {
  review: "--color-admin-teal",
  interview: "--color-admin-cyan",
  testcase: "--color-admin-warn",
  practice: "--color-admin-slate",
};

/** 38400000 becomes "38,4 tr", the unit the mockup used for every token figure. */
function formatMillions(tokens: number): string {
  return `${(tokens / 1_000_000).toFixed(1).replace(".", ",")} tr`;
}

export function AdminAiUsageView() {
  return (
    <QueryBoundary query={useAiUsagePage()}>
      {(page) => <AdminAiUsageViewInner page={page} />}
    </QueryBoundary>
  );
}

function AdminAiUsageViewInner({ page }: { page: AiUsagePage }) {
  const t = useT("adminAiUsage");
  const levelList = useProblemLevels();
  const [range, setRange] = useState<UsageRange>("14d");
  // EVT-5: a reviewed anomaly alert leaves the list. Mock-only: no MarkAiUsageAlertReviewed call yet.
  const [reviewedKeys, setReviewedKeys] = useState<ReadonlySet<string>>(
    () => new Set(),
  );
  const budgetSettings = aiBudgetSettings.use();
  const tokenBudget = aiTokenBudget.use();
  const modelPrices = useModelPrices();

  const days = page.daily[range];
  // "Sắp cạn hạn mức" would contradict a budget that is already locked, so the lock replaces it.
  const alerts = (tokenBudget.locked
    ? page.alerts.map((alert): UsageAlert =>
        alert.key === "budgetRunningOut"
          ? { key: "budgetLocked", tone: "negative" }
          : alert,
      )
    : page.alerts
  ).filter((alert) => !reviewedKeys.has(alert.key));
  const markReviewed = (key: string) => {
    setReviewedKeys((previous) => new Set(previous).add(key));
    toast.success(t("markReviewedDone"));
  };
  const budget = {
    used: formatMillions(tokenBudget.usedTokens),
    cap: formatMillions(tokenBudget.limitTokens),
    left: formatMillions(
      Math.max(0, tokenBudget.limitTokens - tokenBudget.usedTokens),
    ),
    percent: Math.min(
      100,
      Math.round((tokenBudget.usedTokens / tokenBudget.limitTokens) * 100),
    ),
  };

  const userColumns: DataTableColumn<TopUser>[] = [
    {
      key: "rank",
      header: "#",
      width: "28px",
      render: (user) => (
        <span className="font-mono text-[11.5px] text-[var(--color-text-subtle)]">
          {user.rank}
        </span>
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
      render: (user) => (
        <span className="font-mono font-semibold">{user.tokens}</span>
      ),
    },
    {
      key: "calls",
      header: t("columnCalls"),
      width: "74px",
      align: "right",
      render: (user) => (
        <span className="font-mono text-[var(--color-text-muted)]">
          {user.calls}
        </span>
      ),
    },
    {
      key: "cost",
      header: t("columnCost"),
      width: "76px",
      align: "right",
      render: (user) => (
        <span className="font-mono text-[var(--color-text-muted)]">
          {modelPrices.length === 0 ? "-" : user.cost}
        </span>
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
            <Badge variant={problemLevelTone(levelList, problem.difficulty)}>
              {problemLevelLabel(levelList, problem.difficulty)}
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
      render: (problem) => (
        <span className="font-mono font-semibold">{problem.tokens}</span>
      ),
    },
    {
      key: "average",
      header: t("columnAveragePerCall"),
      width: "92px",
      align: "right",
      render: (problem) => (
        <span className="font-mono text-[var(--color-text-muted)]">
          {problem.averagePerCall}
        </span>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        actions={
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
        }
      />

      <div className="mb-4 grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,1.6fr)_minmax(300px,1fr)]">
        <Card
          title={t("dailyChartTitle")}
          description={t("dailyChartSubtitle", { days: days.length })}
          className="min-w-0"
        >
          <StackedBarChart
            series={[
              {
                key: "review",
                label: t("feature.review"),
                colorVar: FEATURE_COLOR_VAR.review,
              },
              {
                key: "interview",
                label: t("feature.interview"),
                colorVar: FEATURE_COLOR_VAR.interview,
              },
              {
                key: "testcase",
                label: t("feature.testcase"),
                colorVar: FEATURE_COLOR_VAR.testcase,
              },
              {
                key: "practice",
                label: t("feature.practice"),
                colorVar: FEATURE_COLOR_VAR.practice,
              },
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
              used: budget.used,
              cap: budget.cap,
            })}
            action={
              <Link
                href="/admin/ai-limits"
                className="text-[12.5px] font-semibold text-[var(--color-primary)] hover:underline"
              >
                {t("editLimits")}
              </Link>
            }
          >
            <ProgressBar
              value={budget.percent}
              label={t("budgetTitle")}
              fill={
                budget.percent >= budgetSettings.warnPercent
                  ? "var(--color-admin-warn)"
                  : "var(--color-admin-teal)"
              }
            />
            <p className="mt-2 mb-4 flex justify-between text-[12.5px] text-[var(--color-text-muted)]">
              <span>{t("budgetLeft", { left: budget.left })}</span>
              <span>
                {tokenBudget.locked
                  ? t("budgetLocked")
                  : t("budgetRunOut", { date: page.budget.runOutLabel })}
              </span>
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
            {alerts.length === 0 ? (
              // BD ADM0302 Sheet 6 E NO1: a neutral line, never an empty block.
              <p className="text-[13px] text-[var(--color-text-muted)]">
                {t("noAlerts")}
              </p>
            ) : (
              <div className="flex flex-col gap-2.5">
                {alerts.map((alert) => (
                  <NoticeTile
                    key={alert.key}
                    tone={alert.tone}
                    title={t(`alert.${alert.key}.title`)}
                    // E NO4: only the anomaly alert has a record to mark; budget alerts do not.
                    action={
                      alert.key === "anomalousAccount" ? (
                        <Button
                          variant="ghost"
                          size="sm"
                          className="border border-[var(--color-border)]"
                          onClick={() => markReviewed(alert.key)}
                        >
                          {t("markReviewed")}
                        </Button>
                      ) : undefined
                    }
                  >
                    {t(`alert.${alert.key}.meta`)}
                  </NoticeTile>
                ))}
              </div>
            )}
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
          // BD ADM0302 G NO3 / EVT-8: opens problem_management with no token filter.
          action={
            <Link
              href="/admin/problems"
              className="text-[12.5px] font-semibold text-[var(--color-primary)] hover:underline"
            >
              {t("problemBankLink")}
            </Link>
          }
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
