// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Layout follows 09-layoutBase/Admin - Hàng đợi chấm.dc.html: sticky header (:148-158), KPI tiles
// (:161-175), a 1.6fr / minmax(280px,1fr) grid with the worker cluster left and controls + latency
// right (:177-228), the job table (:230-268), then recent infrastructure events (:270-290).
//
// Rejudge remnants removed (DEC-2026-0828-remove-rejudge-scope): the mockup still has a "Chấm lại"
// priority on two jobs and a "Hàng đợi chấm lại" latency row. Two priority levels remain, which is
// what DEC-2026-0831-judge-orchestration-ops-details settles. The mockup's "Kỳ thi" mislabel was
// already corrected upstream — the toggle reads "Ưu tiên cao" in the source file.
//
// The cluster controls are real controls, not decoration: F4-10 covers operating the queue it
// monitors (pause consumption, toggle autoscaling), per that same decision.
//
// Infrastructure events belong on THIS screen, not the system log — that split is a hard boundary
// (02-bd/screens/admin/admin_system_log.md section 0), so the caption says where the other half is.
"use client";

import { useMemo, useState } from "react";
import {
  fetchQueuePage,
  type ClusterControls,
  type InfraLevel,
  type JobPriority,
  type JobStatus,
  type QueueJob,
  type WorkerStatus,
} from "@/entities/judge-queue";
import { useT } from "@/shared/i18n";
import {
  Badge,
  Button,
  Card,
  DataTable,
  PageHeader,
  ProgressBar,
  RankedProgressList,
  SegmentedTabs,
  SettingRow,
  StatCard,
  Toggle,
  type BadgeVariant,
  type DataTableColumn,
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

const PRIORITY_VARIANT: Record<JobPriority, BadgeVariant> = {
  high: "warn",
  normal: "neutral",
};

const INFRA_VARIANT: Record<InfraLevel, BadgeVariant> = {
  error: "negative",
  warn: "warn",
  info: "blue",
};

const INFRA_COLOR_VAR: Record<InfraLevel, string> = {
  error: "--color-admin-negative",
  warn: "--color-admin-warn",
  info: "--color-accent-blue",
};

type JobFilter = "all" | "waiting" | "failed";
type SortKey = "wait" | "priority";

export function AdminQueueMonitorView() {
  const t = useT("adminQueueMonitor");
  const [page] = useState(fetchQueuePage);
  const [controls, setControls] = useState<ClusterControls>(page.controls);
  const [filter, setFilter] = useState<JobFilter>("all");
  const [sort, setSort] = useState<{ key: SortKey; direction: "asc" | "desc" }>({
    key: "wait",
    direction: "desc",
  });

  const jobs = useMemo(() => {
    const filtered = page.jobs.filter((job) => {
      if (filter === "waiting") return job.status === "waiting";
      if (filter === "failed") return job.status === "sandboxError";
      return true;
    });

    const factor = sort.direction === "asc" ? 1 : -1;
    return [...filtered].sort((left, right) => {
      if (sort.key === "priority") {
        const rank = (job: QueueJob) => (job.priority === "high" ? 1 : 0);
        return (rank(left) - rank(right)) * factor;
      }
      return (left.waitSeconds - right.waitSeconds) * factor;
    });
  }, [page.jobs, filter, sort]);

  const columns: DataTableColumn<QueueJob>[] = [
    {
      key: "job",
      header: t("columnJob"),
      width: "116px",
      render: (job) => (
        <span className="font-mono text-[12.5px] text-[var(--color-text-muted)]">{job.id}</span>
      ),
    },
    {
      key: "submission",
      header: t("columnSubmission"),
      width: "124px",
      render: (job) => <span className="font-mono text-[12.5px]">{job.submissionId}</span>,
    },
    { key: "language", header: t("columnLanguage"), width: "110px", render: (job) => job.language },
    {
      key: "priority",
      header: t("columnPriority"),
      width: "124px",
      sortable: true,
      render: (job) => (
        <Badge variant={PRIORITY_VARIANT[job.priority]}>{t(`priority.${job.priority}`)}</Badge>
      ),
    },
    {
      key: "status",
      header: t("columnStatus"),
      width: "136px",
      render: (job) => (
        <Badge variant={JOB_STATUS_VARIANT[job.status]}>{t(`jobStatus.${job.status}`)}</Badge>
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

  const controlRows: { key: keyof ClusterControls }[] = [
    { key: "paused" },
    { key: "autoscale" },
    { key: "highPriorityFirst" },
  ];

  return (
    <div>
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        actions={
          <Button variant="cta" size="sm">
            {t("refresh")}
          </Button>
        }
      />

      <div className="mb-4 grid gap-4 [grid-template-columns:repeat(auto-fit,minmax(210px,1fr))]">
        {page.kpis.map((kpi) => (
          <StatCard
            key={kpi.key}
            label={t(`kpi.${kpi.key}.label`)}
            value={kpi.value}
            meta={t(`kpi.${kpi.key}.meta`)}
            valueColorVar={kpi.colorVar}
          />
        ))}
      </div>

      <div className="mb-4 grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,1.6fr)_minmax(280px,1fr)]">
        <Card
          title={t("workersTitle")}
          description={t("workersSubtitle", { count: page.workerCount, max: page.maxWorkers })}
          className="min-w-0"
        >
          <div className="grid gap-3 [grid-template-columns:repeat(auto-fit,minmax(220px,1fr))]">
            {page.workers.map((worker) => (
              <div
                key={worker.id}
                className="glass-surface rounded-2xl border border-[var(--color-border)] px-3.5 py-3"
              >
                <div className="mb-2 flex items-center justify-between gap-2">
                  <span className="font-mono text-[12.5px] font-semibold">{worker.id}</span>
                  <Badge variant={WORKER_VARIANT[worker.status]}>
                    {t(`workerStatus.${worker.status}`)}
                  </Badge>
                </div>
                <ProgressBar
                  value={worker.load}
                  height={6}
                  label={t("workerLoadLabel", { id: worker.id })}
                  fill={
                    worker.load > 85
                      ? "linear-gradient(90deg, var(--color-admin-warn), var(--color-admin-negative))"
                      : "linear-gradient(90deg, var(--color-admin-teal), var(--color-admin-cyan))"
                  }
                />
                <p className="mt-2 flex justify-between text-xs text-[var(--color-text-muted)]">
                  <span>{t("workerLoad", { load: worker.load })}</span>
                  <span className="font-mono">{t("workerJobs", { count: worker.jobs })}</span>
                </p>
                <p className="mt-1 text-[11.5px] text-[var(--color-text-subtle)]">{worker.meta}</p>
              </div>
            ))}
          </div>
        </Card>

        <div className="flex min-w-0 flex-col gap-4">
          <Card title={t("controlsTitle")}>
            <div className="flex flex-col gap-2.5">
              {controlRows.map(({ key }) => (
                <SettingRow
                  key={key}
                  label={t(`control.${key}.label`)}
                  description={t(`control.${key}.meta`)}
                >
                  <Toggle
                    checked={controls[key]}
                    onCheckedChange={(value) =>
                      setControls((previous) => ({ ...previous, [key]: value }))
                    }
                    label={t(`control.${key}.label`)}
                  />
                </SettingRow>
              ))}
            </div>
          </Card>

          <Card title={t("latencyTitle")}>
            <RankedProgressList
              numbered={false}
              items={page.latency.map((item) => ({
                label: t(`latencyQueue.${item.priority}`),
                value: item.percent,
                valueLabel: item.label,
                colorVar:
                  item.percent > 70 ? "--color-admin-warn" : "--color-admin-teal",
              }))}
            />
          </Card>
        </div>
      </div>

      <Card
        title={t("jobsTitle")}
        description={t("jobsSubtitle")}
        className="mb-4 min-w-0"
        action={
          <SegmentedTabs
            label={t("jobFilterLabel")}
            value={filter}
            onValueChange={setFilter}
            options={[
              { value: "all", label: t("filterAll") },
              { value: "waiting", label: t("jobStatus.waiting") },
              { value: "failed", label: t("filterFailed") },
            ]}
          />
        }
      >
        <DataTable
          caption={t("jobsTitle")}
          columns={columns}
          rows={jobs}
          rowKey={(job) => job.id}
          emptyMessage={t("emptyJobs")}
          minWidth={780}
          sort={{
            key: sort.key,
            direction: sort.direction,
            onSortChange: (key, direction) =>
              setSort({ key: key as SortKey, direction }),
          }}
        />
      </Card>

      <Card title={t("infraTitle")} description={t("infraSubtitle")}>
        <ul aria-label={t("infraTitle")} className="flex flex-col gap-0.5">
          {page.infraEvents.map((event) => (
            <li
              key={event.id}
              className="grid grid-cols-[66px_84px_minmax(180px,1fr)] items-start gap-3 rounded-xl border-l-2 px-2.5 py-2.5 hover:bg-[var(--color-row-hover)]"
              style={{ borderLeftColor: `var(${INFRA_COLOR_VAR[event.level]})` }}
            >
              <span className="pt-px font-mono text-xs text-[var(--color-text-subtle)]">
                {event.time}
              </span>
              <Badge variant={INFRA_VARIANT[event.level]} className="w-full justify-center font-mono">
                {t(`infraLevel.${event.level}`)}
              </Badge>
              <span className="min-w-0">
                <span className="block text-[13px] font-semibold text-pretty">{event.message}</span>
                <span className="mt-0.5 block font-mono text-[11.5px] text-[var(--color-text-subtle)]">
                  {event.service}
                </span>
              </span>
            </li>
          ))}
        </ul>
      </Card>
    </div>
  );
}
