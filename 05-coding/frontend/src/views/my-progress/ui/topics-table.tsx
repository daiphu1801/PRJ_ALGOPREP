// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Khu vực B, using shared/ui DataTable — a real <table> exactly per the DD 9-sheet convention this
// column already follows for the Admin screens.
"use client";

import { useT } from "@/shared/i18n";
import { Card, DataTable, ProgressBar, type DataTableColumn } from "@/shared/ui";
import { useTopicProgress, type ProgressRange, type TopicProgress } from "@/entities/progress";

export function TopicsTable({ range }: { range: ProgressRange }) {
  const t = useT("myProgress");
  const query = useTopicProgress(range);

  const columns: DataTableColumn<TopicProgress>[] = [
    { key: "topicName", header: t("topics.colTopic"), render: (row) => row.topicName },
    {
      key: "solved",
      header: t("topics.colSolved"),
      width: "34%",
      render: (row) => (
        <div className="flex items-center gap-2">
          <ProgressBar
            value={row.solvedCount}
            max={Math.max(row.totalCount, 1)}
            label={t("topics.colSolved")}
            fill="var(--color-primary)"
            height={6}
            className="flex-1"
          />
          <span className="font-mono text-xs whitespace-nowrap text-[var(--color-text-muted)]">
            {row.solvedCount} / {row.totalCount}
          </span>
        </div>
      ),
    },
    {
      key: "acRate",
      header: t("topics.colAcRate"),
      align: "right",
      render: (row) => (row.acRate != null ? `${row.acRate}%` : "-"),
    },
    {
      key: "lastSubmittedAt",
      header: t("topics.colLast"),
      align: "right",
      render: (row) => (row.lastSubmittedAt ? new Date(row.lastSubmittedAt).toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit" }) : "-"),
    },
  ];

  return (
    <Card title={t("topics.title")} description={t("topics.orderNote")}>
      <DataTable
        caption={t("topics.title")}
        columns={columns}
        rows={query.data ?? []}
        rowKey={(row) => row.topicId}
        status={query.isLoading ? "loading" : query.isError ? "error" : "ready"}
        emptyMessage={t("emptyGeneric")}
        errorMessage={t("errorGeneric")}
        loadingRowCount={7}
        minWidth={480}
      />
    </Card>
  );
}
