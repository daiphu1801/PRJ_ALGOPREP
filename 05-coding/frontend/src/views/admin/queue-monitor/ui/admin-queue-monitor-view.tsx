"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import {
  QUEUE_KEY,
  useQueueControl,
  useQueueInfra,
  useQueueJobs,
  useQueueLive,
  useQueueSettings,
  useQueueSummary,
  useSaveQueueSettings,
  type ClusterControls,
  type JobStatus,
  type QueueJob,
  type QueueTab,
  type WorkerStatus,
} from "../api";
import { GradingCard } from "./grading-card";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import { useMediaQuery } from "@/shared/lib/use-media-query";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  DataTable,
  PageContainer,
  PageHeader,
  QueryError,
  Pagination,
  ParamsDialog,
  ProgressBar,
  SegmentedTabs,
  SettingRow,
  StatusStrip,
  Toggle,
  type BadgeVariant,
  type DataTableColumn,
  type StatusStripTone,
} from "@/shared/ui";

const WORKER_VARIANT: Record<WorkerStatus, BadgeVariant> = {
  running: "success",
  idle: "blue",
  overloaded: "warn",
};

const JOB_STATUS_VARIANT: Record<JobStatus, BadgeVariant> = {
  running: "blue",
  waiting: "neutral",
  sandboxError: "negative",
};

const STALE_AFTER_MS = 60_000;
const LINK_CLASS =
  "text-[12.5px] font-semibold text-[var(--color-primary)] hover:underline";

type ControlKey = keyof ClusterControls;

/** "HH:mm:ss" in the viewer's local time. */
function clock(epochMs: number): string {
  return new Date(epochMs).toLocaleTimeString("vi-VN", { hour12: false });
}

export function AdminQueueMonitorView() {
  const { poll, fallback } = useQueueLive();
  return <QueueMonitorInner poll={poll} fallback={fallback} />;
}

function QueueMonitorInner({
  poll,
  fallback,
}: {
  poll: number | false;
  fallback: boolean;
}) {
  const t = useT("adminQueueMonitor");
  const errorMessage = useApiErrorMessage();
  const cache = useQueryClient();
  const compact = useMediaQuery("(max-width: 720px)");
  const [editingParams, setEditingParams] = useState(false);
  const [confirming, setConfirming] = useState<ControlKey | null>(null);
  const [tab, setTab] = useState<QueueTab>("waiting");
  const [pageNumber, setPageNumber] = useState(1);
  const [direction, setDirection] = useState<"asc" | "desc">("desc");
  const [refreshing, setRefreshing] = useState(false);
  // Ticks so the "stale" state appears without any user action.
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 10_000);
    return () => clearInterval(timer);
  }, []);

  const jobsQuery = useQueueJobs(tab, pageNumber, direction, poll);
  const jobs = jobsQuery.data;
  const summaryQuery = useQueueSummary(poll);
  const settingsQuery = useQueueSettings(poll);
  const infraQuery = useQueueInfra(poll);
  const summary = summaryQuery.data;
  const infra = infraQuery.data ?? summary?.infra;
  const settings = settingsQuery.data ?? summary?.settings;
  const control = useQueueControl();
  const saveSettings = useSaveQueueSettings();

  const stale =
    summary !== undefined && now - summary.generatedAt > STALE_AFTER_MS;
  const tone: StatusStripTone = summary?.state === "ok" ? "success" : "warn";
  const stateText =
    summary?.state === "backlog"
      ? t("state.backlog", { count: summary.pendingCount })
      : summary && t(`state.${summary.state}`);
  const latencyPercent =
    summary?.latency.seconds == null || !settings
      ? null
      : Math.min(
          100,
          (summary.latency.seconds / settings.latencyCapSeconds) * 100,
        );

  async function refresh() {
    setRefreshing(true);
    try {
      const filters = { queryKey: QUEUE_KEY, type: "active" as const };
      await cache.invalidateQueries(filters);
      const queries = cache.getQueryCache().findAll(filters);
      const failed = queries.filter(
        (query) => query.state.status === "error",
      ).length;
      setNow(Date.now());
      if (failed === 0) toast.success(t("refreshDone"));
      else if (failed === queries.length) toast.error(t("refreshFailed"));
      else toast.warning(t("refreshPartial"));
    } finally {
      setRefreshing(false);
    }
  }

  function apply(key: ControlKey, value: boolean) {
    if (!summary || summaryQuery.isError) return;
    control.mutate(
      { key, value, expected: summary.controls[key] },
      {
        onSuccess: () =>
          toast.success(t(`control.${key}.${value ? "doneOn" : "doneOff"}`)),
        onError: (error) => {
          toast.error(errorMessage(error));
          void cache.invalidateQueries({ queryKey: QUEUE_KEY });
        },
      },
    );
  }

  // Pausing and turning autoscale off are the two risky directions; the reverse applies at once.
  function requestChange(key: ControlKey, value: boolean) {
    const risky = key === "paused" ? value : !value;
    if (risky) setConfirming(key);
    else apply(key, value);
  }

  const columns: DataTableColumn<QueueJob>[] = [
    {
      key: "submission",
      header: t("columnSubmission"),
      render: (job) => (
        <span className="font-mono text-[12.5px] wrap-anywhere">
          {job.submissionId}
        </span>
      ),
    },
    {
      key: "language",
      header: t("columnLanguage"),
      width: "130px",
      render: (job) => job.language,
    },
    {
      key: "status",
      header: t("columnStatus"),
      width: "140px",
      render: (job) => (
        <Badge variant={JOB_STATUS_VARIANT[job.status]}>
          {t(`jobStatus.${job.status}`)}
        </Badge>
      ),
    },
    {
      key: "wait",
      header: t("columnWait"),
      width: "88px",
      align: "right",
      sortable: jobs?.sortable,
      render: (job) => (
        <span className="font-mono text-[12.5px] whitespace-nowrap text-[var(--color-text-muted)]">
          {job.waitLabel}
        </span>
      ),
    },
  ];

  const controlRows: ControlKey[] = ["paused", "autoscale"];
  const emptyJobs = t(`emptyJobsByTab.${tab}`);

  return (
    <PageContainer variant="content" className="@container">
      <PageHeader
        title={t("title")}
        className="static border-0 bg-transparent shadow-none"
        actions={
          <>
            <Button
              variant="ghost"
              size="sm"
              className="border border-[var(--color-border)]"
              disabled={!settings || settingsQuery.isError}
              onClick={() => setEditingParams(true)}
            >
              {t("params.open")}
            </Button>
            <Button
              variant="ghost"
              size="sm"
              disabled={refreshing}
              onClick={refresh}
            >
              {t(refreshing ? "refreshing" : "refresh")}
            </Button>
          </>
        }
      />

      <StatusStrip
        className="rounded-lg"
        tone={
          summaryQuery.isError || stale ? "negative" : summary ? tone : "warn"
        }
        title={
          summaryQuery.isError
            ? t("state.unavailable")
            : stale
              ? t("state.stale")
              : (stateText ?? t("loading"))
        }
        detail={
          stale
            ? t("state.staleHint")
            : fallback
              ? t("state.fallback")
              : undefined
        }
        trailing={
          summary && (
            <span suppressHydrationWarning>
              {t("updatedAt", { time: clock(summary.generatedAt) })}
            </span>
          )
        }
      />

      {summary && (
        <dl
          aria-label={t("metricsLabel")}
          className="grid grid-cols-2 gap-x-4 gap-y-3 border-b border-[var(--color-border)] px-4 pb-4 @min-[670px]:grid-cols-4"
        >
          {summary.grading && <GradingCard grading={summary.grading} />}
          <div>
            <dt className="text-xs text-[var(--color-text-muted)]">
              {t("latencyTitle")}
            </dt>
            <dd className="mt-1 text-sm font-semibold tabular-nums">
              {summary.latency.seconds == null
                ? t("latencyUnavailable")
                : summary.latency.label}
              {latencyPercent !== null && (
                <ProgressBar
                  value={latencyPercent}
                  height={4}
                  label={t("latencyTitle")}
                  className="mt-1.5 max-w-44"
                  fill={
                    latencyPercent > 70
                      ? "var(--color-admin-warn)"
                      : "var(--color-admin-teal)"
                  }
                />
              )}
            </dd>
          </div>
        </dl>
      )}

      <section aria-labelledby="queue-jobs-title" className="min-w-0 px-4 py-4">
        <h2 id="queue-jobs-title" className="mb-3 text-base font-semibold">
          {t("jobsTitle")}
        </h2>
        <SegmentedTabs
          label={t("jobTabsLabel")}
          value={tab}
          onValueChange={(next) => {
            setTab(next);
            setPageNumber(1);
          }}
          className="mb-3 w-fit flex-wrap"
          options={[
            {
              value: "waiting",
              label:
                jobs && !jobsQuery.isError
                  ? t("tabWaiting", { count: jobs.counts.waiting })
                  : t("tabLabels.waiting"),
            },
            {
              value: "failed",
              label:
                jobs && !jobsQuery.isError
                  ? t("tabFailed", { count: jobs.counts.failed })
                  : t("tabLabels.failed"),
            },
            {
              value: "all",
              label:
                jobs && !jobsQuery.isError
                  ? t("tabAll", { count: jobs.counts.all })
                  : t("tabLabels.all"),
            },
          ]}
        />
        {jobsQuery.isError ? (
          <QueryError query={jobsQuery} />
        ) : !jobsQuery.isLoading && !jobs?.items.length ? (
          <p
            role="status"
            className="py-2 text-sm text-[var(--color-text-muted)]"
          >
            {emptyJobs}
          </p>
        ) : compact && jobs?.items.length ? (
          <>
            {jobs.sortable && (
              <Button
                variant="ghost"
                size="sm"
                className="mb-2 border border-[var(--color-border)]"
                onClick={() =>
                  setDirection(direction === "desc" ? "asc" : "desc")
                }
              >
                {t(direction === "desc" ? "sortWaitDesc" : "sortWaitAsc")}
              </Button>
            )}
            <ul className="space-y-2" aria-label={t("jobsTitle")}>
              {jobs.items.map((job) => (
                <li
                  key={job.id}
                  className="rounded-lg border border-[var(--color-border)] p-2.5"
                >
                  <dl className="grid grid-cols-2 gap-2">
                    {columns.map((column) => (
                      <div
                        key={column.key}
                        className={
                          column.key === "submission"
                            ? "col-span-2 min-w-0"
                            : "min-w-0"
                        }
                      >
                        <dt className="text-xs text-[var(--color-text-muted)]">
                          {column.header}
                        </dt>
                        <dd className="mt-0.5 text-sm">{column.render(job)}</dd>
                      </div>
                    ))}
                  </dl>
                </li>
              ))}
            </ul>
          </>
        ) : (
          <DataTable
            caption={t("jobsTitle")}
            columns={columns}
            rows={jobs?.items ?? []}
            rowKey={(job) => job.id}
            status={jobsQuery.isLoading ? "loading" : "ready"}
            emptyMessage={emptyJobs}
            minWidth={520}
            sort={
              jobs?.sortable
                ? {
                    key: "wait",
                    direction,
                    onSortChange: (_key, next) => setDirection(next),
                  }
                : undefined
            }
          />
        )}
        {!jobsQuery.isError && jobs && jobs.total > jobs.pageSize ? (
          <Pagination
            page={jobs.page}
            pageSize={jobs.pageSize}
            total={jobs.total}
            onPageChange={setPageNumber}
            summary={t("pageSummary", {
              from: (jobs.page - 1) * jobs.pageSize + 1,
              to: Math.min(jobs.page * jobs.pageSize, jobs.total),
              total: jobs.total,
            })}
            previousLabel={t("pagePrevious")}
            nextLabel={t("pageNext")}
          />
        ) : null}
      </section>

      <section
        aria-label={t("infraTitle")}
        className="flex flex-wrap items-baseline gap-x-3 gap-y-1 border-y border-[var(--color-border)] px-4 py-3 text-sm"
      >
        <span className="font-semibold">{t("infraTitle")}</span>
        <span className="text-[var(--color-text-muted)]">
          {infraQuery.isError
            ? t("infraUnavailable")
            : infra
              ? t("infraSummary", { warn: infra.warn, error: infra.error })
              : t("loading")}
        </span>
        <Link
          href={
            !infraQuery.isError && infra && infra.error > 0
              ? "/admin/infra-events?level=error"
              : "/admin/infra-events"
          }
          className={`${LINK_CLASS} ms-auto`}
        >
          {t(
            !infraQuery.isError && infra && infra.error > 0
              ? "infraErrorLink"
              : "infraLink",
          )}
        </Link>
      </section>

      {summary && summary.workers.length > 0 ? (
        <Card
          title={t("workersTitle")}
          description={t("workersSubtitle", {
            count: summary.workerCount,
            max: summary.maxWorkers,
          })}
          className="min-w-0"
        >
          <ul aria-label={t("workersTitle")} className="flex flex-col gap-1">
            {summary.workers.map((worker) => (
              <li
                key={worker.id}
                className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-x-3 gap-y-2 rounded-xl px-2.5 py-2 hover:bg-[var(--color-row-hover)] @min-[850px]:grid-cols-[minmax(0,1fr)_90px_minmax(80px,1fr)_56px]"
              >
                <span className="min-w-0">
                  <span className="block font-mono text-[12.5px] font-semibold">
                    {worker.id}
                  </span>
                  <span className="block text-[11.5px] text-[var(--color-text-subtle)]">
                    {worker.meta}
                  </span>
                </span>
                <Badge
                  variant={WORKER_VARIANT[worker.status]}
                  className="justify-center"
                >
                  {t(`workerStatus.${worker.status}`)}
                </Badge>
                <span className="flex items-center gap-2">
                  <ProgressBar
                    value={worker.load}
                    height={6}
                    label={t("workerLoadLabel", { id: worker.id })}
                    className="flex-1"
                    fill={
                      worker.load > 85
                        ? "linear-gradient(90deg, var(--color-admin-warn), var(--color-admin-negative))"
                        : "linear-gradient(90deg, var(--color-admin-teal), var(--color-admin-cyan))"
                    }
                  />
                  <span className="w-9 text-right font-mono text-xs text-[var(--color-text-muted)]">
                    {worker.load}%
                  </span>
                </span>
                <span className="text-right font-mono text-xs text-[var(--color-text-muted)]">
                  {t("workerJobs", { count: worker.jobs })}
                </span>
              </li>
            ))}
          </ul>
        </Card>
      ) : null}

      <details className="border-b border-[var(--color-border)]">
        <summary className="cursor-pointer rounded-sm px-4 py-3 text-sm font-semibold">
          {t("controlsTitle")}
        </summary>
        <div className="space-y-3 px-4 pb-4">
          {summary?.grading?.consumers && (
            <dl className="grid gap-3 @min-[620px]:grid-cols-[minmax(0,1fr)_minmax(0,3fr)]">
              <GradingCard grading={summary.grading} details />
            </dl>
          )}
          {summary && !summaryQuery.isError ? (
            <div className="grid gap-2.5 @min-[675px]:grid-cols-2">
              {controlRows.map((key) => (
                <SettingRow
                  key={key}
                  label={t(`control.${key}.label`)}
                  description={t(
                    key === "autoscale" && !settings
                      ? "control.autoscale.metaUnknown"
                      : `control.${key}.meta`,
                    {
                      threshold: settings?.autoscaleThreshold ?? 0,
                    },
                  )}
                >
                  <Toggle
                    checked={summary.controls[key]}
                    disabled={control.isPending}
                    onCheckedChange={(value) => requestChange(key, value)}
                    label={t(`control.${key}.label`)}
                  />
                </SettingRow>
              ))}
            </div>
          ) : (
            <p className="text-sm text-[var(--color-text-muted)]">
              {t(summaryQuery.isError ? "state.unavailable" : "loading")}
            </p>
          )}
          {settingsQuery.isError && (
            <p className="text-sm text-[var(--color-text-muted)]">
              {t("settingsUnavailable")}
            </p>
          )}
          <Link href="/admin/system-log" className={LINK_CLASS}>
            {t("historyLink")}
          </Link>
        </div>
      </details>

      <ConfirmDialog
        open={confirming !== null}
        onClose={() => setConfirming(null)}
        onConfirm={() => {
          if (confirming) apply(confirming, confirming === "paused");
          setConfirming(null);
        }}
        title={confirming ? t(`control.${confirming}.confirmTitle`) : ""}
        confirmLabel={t("confirm.yes")}
        cancelLabel={t("confirm.no")}
        destructive
      >
        {confirming ? t(`control.${confirming}.confirmBody`) : null}
      </ConfirmDialog>

      {settings && (
        <ParamsDialog
          open={editingParams}
          onClose={() => setEditingParams(false)}
          title={t("params.title")}
          values={{
            autoscaleThreshold: String(settings.autoscaleThreshold),
            jobsPerPage: String(settings.jobsPerPage),
            errorRetentionHours: String(settings.errorRetentionHours),
            latencyCapSeconds: String(settings.latencyCapSeconds),
          }}
          fields={[
            {
              type: "number",
              key: "autoscaleThreshold",
              label: t("params.autoscaleThreshold"),
              hint: t("params.autoscaleThresholdHint"),
              unit: t("params.unitJobs"),
              min: 1,
            },
            {
              type: "number",
              key: "jobsPerPage",
              label: t("params.jobsPerPage"),
              unit: t("params.unitRows"),
              min: 1,
            },
            {
              type: "number",
              key: "errorRetentionHours",
              label: t("params.errorRetention"),
              hint: t("params.errorRetentionHint"),
              unit: t("params.unitHours"),
              min: 1,
            },
            {
              type: "number",
              key: "latencyCapSeconds",
              label: t("params.latencyCap"),
              hint: t("params.latencyCapHint"),
              unit: t("params.unitSeconds"),
              min: 1,
            },
          ]}
          onSave={async (values) => {
            try {
              await saveSettings.mutateAsync({
                autoscaleThreshold: Number(values.autoscaleThreshold),
                jobsPerPage: Number(values.jobsPerPage),
                errorRetentionHours: Number(values.errorRetentionHours),
                latencyCapSeconds: Number(values.latencyCapSeconds),
              });
            } catch (error) {
              toast.error(errorMessage(error));
              throw error;
            }
          }}
          labels={{
            save: t("params.save"),
            cancel: t("params.cancel"),
            saved: t("params.saved"),
            errorMin: (min) => t("params.errorMin", { min }),
          }}
        />
      )}
    </PageContainer>
  );
}
