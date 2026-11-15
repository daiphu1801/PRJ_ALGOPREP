// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Reworked 2026-10-10 from the 09-layoutBase/Admin - Hàng đợi chấm.dc.html layout (sticky header,
// four worker cards, two toggles, two priority bars, six-column table, event feed), which was too dense
// for the end user. Agreed redesign, in reading order:
//   1. a one-line status strip (state + "Cập nhật lúc" + stale warning) instead of KPI cards;
//   2. the worker cluster as one compact list, next to ONE "Điều khiển" card (latency + two toggles);
//   3. the job table behind count tabs, default "Đang chờ", four columns, paginated;
//   4. infrastructure events moved to their own screen (ADM0404), reached from a link row.
//
// Dropped on purpose: job priority and the "Job" id column (BD ADM0401 Q4/Q5 — no source for the
// former, duplicate of the submission id for the latter), and the second latency queue.
//
// Cluster controls are real controls (F4-10): pausing consumption and turning autoscale off ask for
// confirmation first, because both change how every learner's submission is handled.
//
// One render tree for mock and real data (2026-11-14): the hooks in ../api return the same shape in
// both modes. The worker card shows only when the source has worker data (the real snapshot has none).
"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import {
  QUEUE_KEY,
  useQueueControl,
  useQueueJobs,
  useQueueLive,
  useQueueSummary,
  useSaveQueueSettings,
  type ClusterControls,
  type JobStatus,
  type QueueJob,
  type QueueSummary,
  type QueueTab,
  type WorkerStatus,
} from "../api";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  DataTable,
  PageHeader,
  QueryBoundary,
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
  const poll = useQueueLive();
  const summary = useQueueSummary(poll);
  return (
    <QueryBoundary query={summary}>
      {(data) => <QueueMonitorInner summary={data} poll={poll} />}
    </QueryBoundary>
  );
}

function QueueMonitorInner({
  summary,
  poll,
}: {
  summary: QueueSummary;
  poll: number | false;
}) {
  const t = useT("adminQueueMonitor");
  const errorMessage = useApiErrorMessage();
  const cache = useQueryClient();
  const [editingParams, setEditingParams] = useState(false);
  const [confirming, setConfirming] = useState<ControlKey | null>(null);
  const [tab, setTab] = useState<QueueTab>("waiting");
  const [pageNumber, setPageNumber] = useState(1);
  const [direction, setDirection] = useState<"asc" | "desc">("desc");
  // Ticks so the "stale" state appears without any user action.
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 10_000);
    return () => clearInterval(timer);
  }, []);

  const jobsQuery = useQueueJobs(tab, pageNumber, direction, poll);
  const jobs = jobsQuery.data;
  const control = useQueueControl();
  const saveSettings = useSaveQueueSettings();
  const { settings } = summary;

  const stale = now - summary.generatedAt > STALE_AFTER_MS;
  const tone: StatusStripTone = summary.state === "ok" ? "success" : "warn";
  const stateText =
    summary.state === "backlog"
      ? t("state.backlog", { count: summary.pendingCount })
      : t(`state.${summary.state}`);
  const latencyPercent =
    summary.latency.seconds == null
      ? 0
      : Math.min(
          100,
          (summary.latency.seconds / settings.latencyCapSeconds) * 100,
        );

  function refresh() {
    void cache.invalidateQueries({ queryKey: QUEUE_KEY }).then(() => {
      setNow(Date.now());
      toast.success(t("refreshDone"));
    });
  }

  function apply(key: ControlKey, value: boolean) {
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
        <span className="font-mono text-[12.5px]">{job.submissionId}</span>
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
  const hasWorkers = summary.workers.length > 0;

  return (
    <div>
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        actions={
          <>
            <Button
              variant="ghost"
              size="sm"
              className="border border-[var(--color-border)]"
              onClick={() => setEditingParams(true)}
            >
              {t("params.open")}
            </Button>
            <Button variant="cta" size="sm" onClick={refresh}>
              {t("refresh")}
            </Button>
          </>
        }
      />

      <StatusStrip
        tone={stale ? "negative" : tone}
        title={stale ? t("state.stale") : stateText}
        detail={stale ? t("state.staleHint") : undefined}
        trailing={
          <span suppressHydrationWarning>
            {t("updatedAt", { time: clock(summary.generatedAt) })}
          </span>
        }
      />

      <div
        className={`mb-4 grid grid-cols-1 gap-4 ${hasWorkers ? "lg:grid-cols-[minmax(0,1.4fr)_minmax(280px,1fr)]" : ""}`}
      >
        {hasWorkers ? (
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
                  className="grid grid-cols-[minmax(0,1fr)_90px_minmax(80px,1fr)_56px] items-center gap-3 rounded-xl px-2.5 py-2 hover:bg-[var(--color-row-hover)]"
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

        <Card
          title={t("controlsTitle")}
          action={
            <Link href="/admin/system-log" className={LINK_CLASS}>
              {t("historyLink")}
            </Link>
          }
          className="min-w-0"
        >
          <div className="mb-3">
            <div className="mb-1.5 flex items-baseline justify-between text-[13px]">
              <span className="font-semibold">{t("latencyTitle")}</span>
              <span className="font-mono text-xs text-[var(--color-text-muted)]">
                {summary.latency.label}
              </span>
            </div>
            <ProgressBar
              value={latencyPercent}
              height={6}
              label={t("latencyTitle")}
              fill={
                latencyPercent > 70
                  ? "var(--color-admin-warn)"
                  : "var(--color-admin-teal)"
              }
            />
          </div>
          <div className="flex flex-col gap-2.5">
            {controlRows.map((key) => (
              <SettingRow
                key={key}
                label={t(`control.${key}.label`)}
                description={t(`control.${key}.meta`, {
                  threshold: settings.autoscaleThreshold,
                })}
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
        </Card>
      </div>

      <Card
        title={t("jobsTitle")}
        description={t("jobsSubtitle")}
        className="mb-4 min-w-0"
        action={
          <SegmentedTabs
            label={t("jobTabsLabel")}
            value={tab}
            onValueChange={(next) => {
              setTab(next);
              setPageNumber(1);
            }}
            options={[
              {
                value: "waiting",
                label: t("tabWaiting", { count: jobs?.counts.waiting ?? 0 }),
              },
              {
                value: "failed",
                label: t("tabFailed", { count: jobs?.counts.failed ?? 0 }),
              },
              {
                value: "all",
                label: t("tabAll", { count: jobs?.counts.all ?? 0 }),
              },
            ]}
          />
        }
      >
        {jobsQuery.isError ? <QueryError query={jobsQuery} /> : null}
        <DataTable
          caption={t("jobsTitle")}
          columns={columns}
          rows={jobs?.items ?? []}
          rowKey={(job) => job.id}
          status={
            jobsQuery.isLoading
              ? "loading"
              : jobsQuery.isError
                ? "error"
                : "ready"
          }
          emptyMessage={t("emptyJobs")}
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
        {jobs && jobs.total > jobs.pageSize ? (
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
      </Card>

      <Card className="min-w-0 py-3">
        <p className="flex flex-wrap items-center gap-x-3 gap-y-1 text-[13px]">
          <span className="font-semibold">{t("infraTitle")}</span>
          <span className="text-[var(--color-text-muted)]">
            {t("infraSummary", {
              warn: summary.infra.warn,
              error: summary.infra.error,
            })}
          </span>
          <Link
            href={
              summary.infra.error > 0
                ? "/admin/infra-events?level=error"
                : "/admin/infra-events"
            }
            className={`${LINK_CLASS} ml-auto`}
          >
            {t("infraLink")}
          </Link>
        </p>
      </Card>

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
            unit: t("params.unitHours"),
            min: 1,
          },
          {
            type: "number",
            key: "latencyCapSeconds",
            label: t("params.latencyCap"),
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
    </div>
  );
}
