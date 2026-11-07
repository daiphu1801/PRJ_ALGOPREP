"use client";
import Link from "next/link";
import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { subscribeJudge } from "@/shared/api/stomp";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import {
  Button,
  Card,
  ConfirmDialog,
  DataTable,
  PageHeader,
  Pagination,
  ParamsDialog,
  QueryBoundary,
  QueryError,
  SegmentedTabs,
  Toggle,
} from "@/shared/ui";
import {
  readJobs,
  readQueue,
  saveQueueSettings,
  setQueueControl,
  type QueueJobs,
  type QueueSnapshot,
} from "../api/real-queue";

export function RealQueueMonitor() {
  const t = useT("adminQueueMonitor");
  const errorMessage = useApiErrorMessage();
  const cache = useQueryClient();
  const [tab, setTab] = useState<"waiting" | "failed" | "all">("waiting");
  const [page, setPage] = useState(1);
  const [editing, setEditing] = useState(false);
  const [confirming, setConfirming] = useState<"paused" | "autoscale" | null>(
    null,
  );
  const [now, setNow] = useState(() => Date.now());
  const [connected, setConnected] = useState(false);
  const [lastBatch, setLastBatch] = useState(0);
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 10000);
    return () => clearInterval(timer);
  }, []);
  const fallback = !connected || now - lastBatch > 60000;
  const summary = useQuery({
    queryKey: ["judge-admin", "summary"],
    queryFn: readQueue,
    retry: false,
    refetchInterval: fallback ? 15000 : false,
  });
  const jobs = useQuery({
    queryKey: ["judge-admin", "jobs", tab, page],
    queryFn: () => readJobs(tab, page),
    retry: false,
    refetchInterval: fallback ? 15000 : false,
  });
  const control = useMutation({
    mutationFn: ({
      key,
      value,
      expected,
    }: {
      key: "paused" | "autoscale";
      value: boolean;
      expected: boolean;
    }) => setQueueControl(key, value, expected),
    onSuccess: () => {
      void cache.invalidateQueries({ queryKey: ["judge-admin"] });
    },
    onError: (error) => {
      toast.error(errorMessage(error));
      void cache.invalidateQueries({ queryKey: ["judge-admin", "summary"] });
    },
  });
  useEffect(
    () =>
      subscribeJudge("/topic/admin/queue", {
        connected: () => {
          setConnected(true);
          setLastBatch(Date.now());
          void cache.invalidateQueries({ queryKey: ["judge-admin"] });
        },
        disconnected: () => setConnected(false),
        message: (value) => {
          const batch = value as {
            type?: string;
            snapshot?: QueueSnapshot;
            infraSummary?: { warnCount: number; errorCount: number };
          };
          if (
            batch?.type !== "QUEUE_BATCH" ||
            !batch.snapshot ||
            !batch.infraSummary
          )
            return;
          setLastBatch(Date.now());
          cache.setQueryData<Awaited<ReturnType<typeof readQueue>>>(
            ["judge-admin", "summary"],
            (previous) =>
              previous
                ? {
                    ...previous,
                    snapshot: batch.snapshot!,
                    infra: batch.infraSummary!,
                  }
                : previous,
          );
          void cache.invalidateQueries({ queryKey: ["judge-admin", "jobs"] });
        },
      }),
    [cache],
  );
  function request(key: "paused" | "autoscale", value: boolean) {
    if (!summary.data) return;
    if ((key === "paused" && value) || (key === "autoscale" && !value)) {
      setConfirming(key);
      return;
    }
    control.mutate({
      key,
      value,
      expected:
        key === "paused"
          ? summary.data.snapshot.queueConsumptionPaused
          : summary.data.snapshot.autoscaleEnabled,
    });
  }
  const counts = jobs.data?.counts;
  return (
    <div className="space-y-4">
      <PageHeader title={t("title")} description={t("subtitle")} />
      <QueryBoundary query={summary}>
        {({ snapshot, settings, infra }) => (
          <>
            <Card className="space-y-3">
              <p>
                {snapshot.status === "PAUSED"
                  ? t("state.paused")
                  : snapshot.status === "BACKLOGGED"
                    ? t("state.backlog", { count: snapshot.pendingCount })
                    : t("state.ok")}
              </p>
              <p>
                {t("updatedAt", {
                  time: new Date(snapshot.generatedAt).toLocaleTimeString(),
                })}
              </p>
              {now - Date.parse(snapshot.generatedAt) > 60000 ? (
                <p>{t("state.staleHint")}</p>
              ) : null}
              <p>
                {t("latencyTitle")}:{" "}
                {snapshot.avgWaitMillis == null
                  ? "—"
                  : `${Math.round(snapshot.avgWaitMillis / 1000)}s`}
              </p>
              <div className="flex flex-wrap gap-4">
                <Toggle
                  label={t("control.paused.label")}
                  checked={snapshot.queueConsumptionPaused}
                  disabled={control.isPending}
                  onCheckedChange={(value) => request("paused", value)}
                />
                <Toggle
                  label={t("control.autoscale.label")}
                  checked={snapshot.autoscaleEnabled}
                  disabled={control.isPending}
                  onCheckedChange={(value) => request("autoscale", value)}
                />
                <Button onClick={() => setEditing(true)}>
                  {t("params.open")}
                </Button>
                <Button
                  onClick={() =>
                    void cache.invalidateQueries({ queryKey: ["judge-admin"] })
                  }
                >
                  {t("refresh")}
                </Button>
              </div>
              <Link href="/admin/infra-events">
                {t("infraSummary", {
                  warn: infra.warnCount,
                  error: infra.errorCount,
                })}{" "}
                · {t("infraLink")}
              </Link>
            </Card>
            <ParamsDialog
              open={editing}
              onClose={() => setEditing(false)}
              title={t("params.title")}
              values={{
                autoscaleJobThreshold: String(settings.autoscaleJobThreshold),
                jobsPerPage: String(settings.jobsPerPage),
                errorRetentionHours: String(settings.errorRetentionHours),
                latencyCapSeconds: String(settings.latencyCapSeconds),
              }}
              fields={[
                {
                  type: "number",
                  key: "autoscaleJobThreshold",
                  label: t("params.autoscaleThreshold"),
                  min: 1,
                },
                {
                  type: "number",
                  key: "jobsPerPage",
                  label: t("params.jobsPerPage"),
                  min: 1,
                },
                {
                  type: "number",
                  key: "errorRetentionHours",
                  label: t("params.errorRetention"),
                  min: 1,
                },
                {
                  type: "number",
                  key: "latencyCapSeconds",
                  label: t("params.latencyCap"),
                  min: 1,
                },
              ]}
              onSave={async (values) => {
                try {
                  await saveQueueSettings({
                    autoscaleJobThreshold: Number(values.autoscaleJobThreshold),
                    jobsPerPage: Number(values.jobsPerPage),
                    errorRetentionHours: Number(values.errorRetentionHours),
                    latencyCapSeconds: Number(values.latencyCapSeconds),
                  });
                  await cache.invalidateQueries({ queryKey: ["judge-admin"] });
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
            <ConfirmDialog
              open={confirming !== null}
              onClose={() => setConfirming(null)}
              title={confirming ? t(`control.${confirming}.confirmTitle`) : ""}
              confirmLabel={t("confirm.yes")}
              cancelLabel={t("confirm.no")}
              onConfirm={() => {
                if (confirming)
                  control.mutate({
                    key: confirming,
                    value: confirming === "paused",
                    expected:
                      confirming === "paused"
                        ? snapshot.queueConsumptionPaused
                        : snapshot.autoscaleEnabled,
                  });
                setConfirming(null);
              }}
              destructive
            >
              {confirming ? t(`control.${confirming}.confirmBody`) : null}
            </ConfirmDialog>
          </>
        )}
      </QueryBoundary>
      <Card className="space-y-3">
        <SegmentedTabs
          label={t("jobTabsLabel")}
          value={tab}
          onValueChange={(value) => {
            setTab(value);
            setPage(1);
          }}
          options={[
            {
              value: "waiting",
              label: t("tabWaiting", { count: counts?.waiting ?? 0 }),
            },
            {
              value: "failed",
              label: t("tabFailed", { count: counts?.failed ?? 0 }),
            },
            { value: "all", label: t("tabAll", { count: counts?.all ?? 0 }) },
          ]}
        />
        {jobs.isError ? <QueryError query={jobs} /> : null}
        <DataTable<QueueJobs["items"][number]>
          caption={t("jobsTitle")}
          rows={jobs.data?.items ?? []}
          rowKey={(row) => row.submissionId}
          status={jobs.isLoading ? "loading" : jobs.isError ? "error" : "ready"}
          emptyMessage={t("emptyJobs")}
          columns={[
            {
              key: "id",
              header: t("columnSubmission"),
              render: (row) => row.submissionId,
            },
            {
              key: "language",
              header: t("columnLanguage"),
              render: (row) => row.language,
            },
            {
              key: "status",
              header: t("columnStatus"),
              render: (row) =>
                t(
                  `jobStatus.${row.status === "PENDING" ? "waiting" : row.status === "SYSTEM_ERROR" ? "sandboxError" : "running"}`,
                ),
            },
            {
              key: "wait",
              header: t("columnWait"),
              render: (row) =>
                row.waitingMillis == null
                  ? "—"
                  : `${Math.round(row.waitingMillis / 1000)}s`,
            },
          ]}
        />
        <Pagination
          page={jobs.data?.page ?? page}
          pageSize={jobs.data?.pageSize ?? 20}
          total={jobs.data?.totalCount ?? 0}
          onPageChange={setPage}
          summary={t("pageSummary", {
            from: jobs.data?.totalCount
              ? (jobs.data.page - 1) * jobs.data.pageSize + 1
              : 0,
            to: Math.min(
              (jobs.data?.page ?? 1) * (jobs.data?.pageSize ?? 20),
              jobs.data?.totalCount ?? 0,
            ),
            total: jobs.data?.totalCount ?? 0,
          })}
          previousLabel={t("pagePrevious")}
          nextLabel={t("pageNext")}
        />
      </Card>
    </div>
  );
}
