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
"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import {
  fetchQueuePage,
  type ClusterControls,
  type JobStatus,
  type QueueJob,
  type WorkerStatus,
} from "../api";
import { countRecentInfraAlerts } from "@/entities/infra-event";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import { queueSettings } from "../model/settings";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  DataTable,
  PageHeader,
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

type JobTab = "waiting" | "failed" | "all";
type SortKey = "wait";
type ControlKey = keyof ClusterControls;

/** "HH:mm:ss" in the viewer's local time. */
function clock(epochMs: number): string {
  return new Date(epochMs).toLocaleTimeString("vi-VN", { hour12: false });
}

export function AdminQueueMonitorView() {
  const t = useT("adminQueueMonitor");
  const [page, setPage] = useState(fetchQueuePage);
  const [controls, setControls] = useState<ClusterControls>(page.controls);
  const settings = queueSettings.use();
  const [editingParams, setEditingParams] = useState(false);
  const [confirming, setConfirming] = useState<ControlKey | null>(null);
  const [tab, setTab] = useState<JobTab>("waiting");
  const [pageNumber, setPageNumber] = useState(1);
  const [sort, setSort] = useState<{ key: SortKey; direction: "asc" | "desc" }>(
    { key: "wait", direction: "desc" },
  );
  // Ticks so the "stale" state appears without any user action.
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 10_000);
    return () => clearInterval(timer);
  }, []);

  const counts = useMemo(
    () => ({
      waiting: page.jobs.filter((job) => job.status === "waiting").length,
      failed: page.jobs.filter((job) => job.status === "sandboxError").length,
      all: page.jobs.length,
    }),
    [page.jobs],
  );
  const alerts = countRecentInfraAlerts();

  // State priority: paused > an overloaded worker > backlog at or above the autoscale threshold.
  const stale = now - page.generatedAt > STALE_AFTER_MS;
  let tone: StatusStripTone = "success";
  let stateText = t("state.ok");
  if (controls.paused) {
    tone = "warn";
    stateText = t("state.paused");
  } else if (page.workers.some((worker) => worker.status === "overloaded")) {
    tone = "warn";
    stateText = t("state.overloaded");
  } else if (counts.waiting >= settings.autoscaleThreshold) {
    tone = "warn";
    stateText = t("state.backlog", { count: counts.waiting });
  }

  const visibleJobs = useMemo(() => {
    const filtered = page.jobs.filter((job) => {
      if (tab === "waiting") return job.status === "waiting";
      if (tab === "failed") return job.status === "sandboxError";
      return true;
    });
    const factor = sort.direction === "asc" ? 1 : -1;
    // BD F NO10: failed jobs always sink to the bottom whatever the direction. They carry
    // waitSeconds -1, so a plain numeric sort put them FIRST when ascending (UX review B3).
    return [...filtered].sort((a, b) => {
      const aFailed = a.status === "sandboxError";
      const bFailed = b.status === "sandboxError";
      if (aFailed !== bFailed) return aFailed ? 1 : -1;
      return (a.waitSeconds - b.waitSeconds) * factor;
    });
  }, [page.jobs, tab, sort]);

  const lastPage = Math.max(
    1,
    Math.ceil(visibleJobs.length / settings.jobsPerPage),
  );
  const currentPage = Math.min(pageNumber, lastPage);
  const rows = visibleJobs.slice(
    (currentPage - 1) * settings.jobsPerPage,
    currentPage * settings.jobsPerPage,
  );

  function refresh() {
    setPage(fetchQueuePage());
    setNow(Date.now());
    toast.success(t("refreshDone"));
  }

  function apply(key: ControlKey, value: boolean) {
    setControls((previous) => ({ ...previous, [key]: value }));
    toast.success(t(`control.${key}.${value ? "doneOn" : "doneOff"}`));
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
      sortable: true,
      render: (job) => (
        <span className="font-mono text-[12.5px] whitespace-nowrap text-[var(--color-text-muted)]">
          {job.waitLabel}
        </span>
      ),
    },
  ];

  const controlRows: ControlKey[] = ["paused", "autoscale"];
  const latencyPercent = Math.min(
    100,
    (page.latency.seconds / settings.latencyCapSeconds) * 100,
  );

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
            {t("updatedAt", { time: clock(page.generatedAt) })}
          </span>
        }
      />

      <div className="mb-4 grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,1.4fr)_minmax(280px,1fr)]">
        <Card
          title={t("workersTitle")}
          description={t("workersSubtitle", {
            count: page.workerCount,
            max: page.maxWorkers,
          })}
          className="min-w-0"
        >
          <ul aria-label={t("workersTitle")} className="flex flex-col gap-1">
            {page.workers.map((worker) => (
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
                {page.latency.label}
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
                  checked={controls[key]}
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
                label: t("tabWaiting", { count: counts.waiting }),
              },
              {
                value: "failed",
                label: t("tabFailed", { count: counts.failed }),
              },
              { value: "all", label: t("tabAll", { count: counts.all }) },
            ]}
          />
        }
      >
        <DataTable
          caption={t("jobsTitle")}
          columns={columns}
          rows={rows}
          rowKey={(job) => job.id}
          emptyMessage={t("emptyJobs")}
          minWidth={520}
          sort={{
            key: sort.key,
            direction: sort.direction,
            onSortChange: (key, direction) =>
              setSort({ key: key as SortKey, direction }),
          }}
        />
        {visibleJobs.length > settings.jobsPerPage ? (
          <Pagination
            page={currentPage}
            pageSize={settings.jobsPerPage}
            total={visibleJobs.length}
            onPageChange={setPageNumber}
            summary={t("pageSummary", {
              from: (currentPage - 1) * settings.jobsPerPage + 1,
              to: Math.min(
                currentPage * settings.jobsPerPage,
                visibleJobs.length,
              ),
              total: visibleJobs.length,
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
            {t("infraSummary", { warn: alerts.warn, error: alerts.error })}
          </span>
          <Link
            href={
              alerts.error > 0
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
        onSave={(values) =>
          queueSettings.set({
            autoscaleThreshold: Number(values.autoscaleThreshold),
            jobsPerPage: Number(values.jobsPerPage),
            errorRetentionHours: Number(values.errorRetentionHours),
            latencyCapSeconds: Number(values.latencyCapSeconds),
          })
        }
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
