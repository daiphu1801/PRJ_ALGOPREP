// BD ADM0302 (admin_ai_usage). Layout follows 09-layoutBase/Admin - Token AI.dc.html: header with a
// range switch (the mockup's export CTA was dropped 2026-10-10, no F-code behind it), a 1.6fr /
// minmax(300px,1fr) grid holding the daily chart and the budget/alert column, then two ranked tables.
//
// Data: F5 #18 (budget bar, feature shares), #27 (daily chart), #28 and #29 (alerts), #30 and #31
// (rankings), see api/. Every block has its own query, so one failing block shows its own error with a
// retry while the others stay readable (DD ADM0302 section 3).
//
// Two documented behaviours worth not getting wrong:
// - The anomalous-account notice is a SOFT warning for an admin to review, never an automatic lock
//   (DEC-2026-0831-ai-usage-anomaly-alert). Marking it reviewed changes nothing else.
// - The budget panel stays read-only; unlocking belongs to admin_ai_limits (ADM0303), so two screens do not
//   both write `ai_token_budget_configs` (BD section 7).
"use client";

import Link from "next/link";
import { useState } from "react";
import {
  useAiTopProblems,
  useAiTopUsers,
  useAiUsageAlerts,
  useAiUsageDaily,
  useMarkAlertReviewed,
  type AiFeature,
  type TopProblem,
  type TopUser,
  type UsageRange,
} from "../api";
import { AI_FEATURES, useTokenBudget } from "@/entities/ai-budget";
import {
  problemLevelLabel,
  problemLevelTone,
  useProblemLevels,
} from "@/entities/problem";
import { ApiError } from "@/shared/api";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import { BlockError } from "./block-error";
import { toast } from "@/shared/lib/toast-store";
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
  Skeleton,
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

const numberFormat = new Intl.NumberFormat("vi-VN", {
  maximumFractionDigits: 2,
});

/** 38400000 becomes "38,4 tr", the unit the mockup used for every token figure. */
function formatMillions(tokens: number): string {
  return `${(tokens / 1_000_000).toFixed(1).replace(".", ",")} tr`;
}

/** Table figures: "1,84 tr" from one million up, the plain count below it. */
function formatTokens(tokens: number): string {
  return tokens >= 1_000_000
    ? `${numberFormat.format(tokens / 1_000_000)} tr`
    : numberFormat.format(tokens);
}

/** "2026-08-28" becomes "28/08", the form the mockup used. */
function dayMonth(iso: string): string {
  const [, month, day] = iso.split("-");
  return `${day}/${month}`;
}

/** "2026-08-28" becomes "28/8", short enough for a chart axis. */
function axisLabel(iso: string): string {
  const [, month, day] = iso.split("-");
  return `${Number(day)}/${Number(month)}`;
}

export function AdminAiUsageView() {
  const t = useT("adminAiUsage");
  const [range, setRange] = useState<UsageRange>("14d");

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
        <DailyChartCard range={range} />
        <div className="flex min-w-0 flex-col gap-4">
          <BudgetCard />
          <AlertsCard />
        </div>
      </div>

      <div className="grid gap-4 [grid-template-columns:repeat(auto-fit,minmax(380px,1fr))]">
        <TopUsersCard />
        <TopProblemsCard />
      </div>
    </div>
  );
}

function BlockSkeleton() {
  const t = useT("common");
  return (
    <div role="status" aria-label={t("loading")}>
      <Skeleton className="h-32 w-full" />
    </div>
  );
}

function DailyChartCard({ range }: { range: UsageRange }) {
  const t = useT("adminAiUsage");
  const query = useAiUsageDaily(range);
  const days = query.data;

  return (
    <Card
      title={t("dailyChartTitle")}
      description={
        days ? t("dailyChartSubtitle", { days: days.length }) : undefined
      }
      className="min-w-0"
    >
      {query.isError ? (
        <BlockError query={query} />
      ) : !days ? (
        <BlockSkeleton />
      ) : (
        <StackedBarChart
          series={AI_FEATURES.map((feature) => ({
            key: feature,
            label: t(`feature.${feature}`),
            colorVar: FEATURE_COLOR_VAR[feature],
          }))}
          bars={days.map((day) => ({
            label: axisLabel(day.date),
            title: `${axisLabel(day.date)}: ${formatTokens(
              AI_FEATURES.reduce((sum, key) => sum + day.values[key], 0),
            )}`,
            values: day.values,
          }))}
        />
      )}
    </Card>
  );
}

function BudgetCard() {
  const t = useT("adminAiUsage");
  const query = useTokenBudget();
  const budget = query.data;

  const used = budget?.usedTokens ?? 0;
  const limit = budget?.limitTokens ?? 0;
  // 0 means "not configured": no division by zero, the bar stays empty.
  const percent =
    limit > 0 ? Math.min(100, Math.round((used / limit) * 100)) : 0;
  const featureTotal = budget
    ? AI_FEATURES.reduce((sum, key) => sum + budget.featureTokens[key], 0)
    : 0;

  return (
    <Card
      title={t("budgetTitle")}
      description={
        budget
          ? t("budgetSubtitle", {
              used: formatMillions(used),
              cap: formatMillions(limit),
            })
          : undefined
      }
      action={
        <Link
          href="/admin/ai-limits"
          className="text-[12.5px] font-semibold text-[var(--color-primary)] hover:underline"
        >
          {t("editLimits")}
        </Link>
      }
    >
      {query.isError ? (
        <BlockError query={query} />
      ) : !budget ? (
        <BlockSkeleton />
      ) : (
        <>
          <ProgressBar
            value={percent}
            label={t("budgetTitle")}
            fill={
              percent >= budget.warnPercent
                ? "var(--color-admin-warn)"
                : "var(--color-admin-teal)"
            }
          />
          <p className="mt-2 mb-4 flex justify-between text-[12.5px] text-[var(--color-text-muted)]">
            <span>
              {t("budgetLeft", {
                left: formatMillions(Math.max(0, limit - used)),
              })}
            </span>
            {budget.locked ? (
              <span>{t("budgetLocked")}</span>
            ) : budget.projectedRunOutDate ? (
              <span>
                {t("budgetRunOut", {
                  date: dayMonth(budget.projectedRunOutDate),
                })}
              </span>
            ) : null}
          </p>
          <RankedProgressList
            numbered={false}
            items={AI_FEATURES.map((feature) => ({
              label: t(`feature.${feature}`),
              value:
                featureTotal > 0
                  ? Math.round(
                      (budget.featureTokens[feature] / featureTotal) * 100,
                    )
                  : 0,
              valueLabel: formatMillions(budget.featureTokens[feature]),
              colorVar: FEATURE_COLOR_VAR[feature],
            }))}
          />
        </>
      )}
    </Card>
  );
}

function AlertsCard() {
  const t = useT("adminAiUsage");
  const toMessage = useApiErrorMessage();
  const query = useAiUsageAlerts();
  const budget = useTokenBudget();
  const markReviewed = useMarkAlertReviewed();

  function review(alertId: string) {
    markReviewed.mutate(alertId, {
      onSuccess: () => toast.success(t("markReviewedDone")),
      onError: (error) => {
        toast.error(toMessage(error));
        // AIR-120: the alert is gone already, so the list is stale.
        if (error instanceof ApiError && error.code === "AIR-120")
          void query.refetch();
      },
    });
  }

  const alerts = query.data;
  // "Sắp cạn hạn mức" would contradict a budget that is already locked, so the lock replaces it.
  const budgetKey = budget.data?.locked ? "budgetLocked" : "budgetRunningOut";
  const runOut = budget.data?.projectedRunOutDate;

  return (
    <Card title={t("alertsTitle")}>
      {query.isError ? (
        <BlockError query={query} />
      ) : !alerts ? (
        <BlockSkeleton />
      ) : alerts.length === 0 ? (
        // BD ADM0302 Sheet 6 E NO1: a neutral line, never an empty block.
        <p className="text-[13px] text-[var(--color-text-muted)]">
          {t("noAlerts")}
        </p>
      ) : (
        <div className="flex flex-col gap-2.5">
          {alerts.map((alert) =>
            alert.kind === "budget" ? (
              <NoticeTile
                key="budget"
                tone={budgetKey === "budgetLocked" ? "negative" : "warn"}
                title={t(`alert.${budgetKey}.title`)}
              >
                {budgetKey === "budgetRunningOut" && !runOut
                  ? t("alert.budgetRunningOut.metaNoDate")
                  : t(`alert.${budgetKey}.meta`, {
                      date: runOut ? dayMonth(runOut) : "",
                    })}
              </NoticeTile>
            ) : (
              <NoticeTile
                key={alert.alertId}
                tone="negative"
                title={t("alert.anomalousAccount.title")}
                // E NO4: only the anomaly alert has a record to mark; the budget alert does not.
                action={
                  <Button
                    variant="ghost"
                    size="sm"
                    className="border border-[var(--color-border)]"
                    disabled={
                      markReviewed.isPending &&
                      markReviewed.variables === alert.alertId
                    }
                    onClick={() => review(alert.alertId)}
                  >
                    {t("markReviewed")}
                  </Button>
                }
              >
                {t("alert.anomalousAccount.meta", {
                  name: alert.displayName ?? t("alert.unknownAccount"),
                  count: alert.callCount,
                  ratio: numberFormat.format(alert.ratio),
                })}
              </NoticeTile>
            ),
          )}
        </div>
      )}
    </Card>
  );
}

function TopUsersCard() {
  const t = useT("adminAiUsage");
  const query = useAiTopUsers();

  const columns: DataTableColumn<TopUser & { rank: number }>[] = [
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
        <span className="block truncate font-semibold">{user.name ?? "-"}</span>
      ),
    },
    {
      key: "tokens",
      header: t("columnTokens"),
      width: "84px",
      align: "right",
      render: (user) => (
        <span className="font-mono font-semibold">
          {formatTokens(user.tokens)}
        </span>
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
          {user.costUsd === null
            ? "-"
            : `${numberFormat.format(user.costUsd)} USD`}
        </span>
      ),
    },
  ];

  return (
    <Card
      title={t("topUsersTitle")}
      description={t("topUsersSubtitle")}
      className="min-w-0 px-[18px]"
    >
      {query.isError ? (
        <BlockError query={query} />
      ) : !query.data ? (
        <BlockSkeleton />
      ) : (
        <DataTable
          caption={t("topUsersTitle")}
          columns={columns}
          rows={query.data.map((user, index) => ({ ...user, rank: index + 1 }))}
          rowKey={(user) => user.userId}
          emptyMessage={t("emptyUsers")}
          minWidth={420}
        />
      )}
    </Card>
  );
}

function TopProblemsCard() {
  const t = useT("adminAiUsage");
  const levelList = useProblemLevels();
  const query = useAiTopProblems();

  const columns: DataTableColumn<TopProblem & { rank: number }>[] = [
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
          <span className="block truncate font-semibold">
            {problem.title ?? "-"}
          </span>
          <span className="mt-0.5 flex items-center gap-2 text-[11.5px] text-[var(--color-text-subtle)]">
            {problem.levelCode ? (
              <Badge variant={problemLevelTone(levelList, problem.levelCode)}>
                {problem.levelName ??
                  problemLevelLabel(levelList, problem.levelCode)}
              </Badge>
            ) : null}
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
        <span className="font-mono font-semibold">
          {formatTokens(problem.tokens)}
        </span>
      ),
    },
    {
      key: "average",
      header: t("columnAveragePerCall"),
      width: "92px",
      align: "right",
      render: (problem) => (
        <span className="font-mono text-[var(--color-text-muted)]">
          {numberFormat.format(problem.avgTokensPerCall)}
        </span>
      ),
    },
  ];

  return (
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
      {query.isError ? (
        <BlockError query={query} />
      ) : !query.data ? (
        <BlockSkeleton />
      ) : (
        <DataTable
          caption={t("topProblemsTitle")}
          columns={columns}
          rows={query.data.map((problem, index) => ({
            ...problem,
            rank: index + 1,
          }))}
          rowKey={(problem) => problem.problemId}
          emptyMessage={t("emptyProblems")}
          minWidth={420}
        />
      )}
    </Card>
  );
}
