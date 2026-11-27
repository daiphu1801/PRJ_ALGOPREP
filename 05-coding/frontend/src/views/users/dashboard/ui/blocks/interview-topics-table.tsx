// Khu vuc F NO 10-14 of USR0601: question review progress per interview-bank topic (F6 #8).
// F6-owned, so it fails alone; no range (the endpoint takes none).
"use client";

import {
  useInterviewTopicProgress,
  type InterviewTopicProgress,
} from "@/entities/interview-question";
import { useT } from "@/shared/i18n";
import { formatDateVN } from "@/shared/lib";
import {
  Card,
  DataTable,
  ProgressBar,
  type DataTableColumn,
} from "@/shared/ui";

export function InterviewTopicsTable() {
  const t = useT("dashboard");
  const query = useInterviewTopicProgress();

  const columns: DataTableColumn<InterviewTopicProgress>[] = [
    {
      key: "topicName",
      header: t("interviewTopics.colTopic"),
      render: (row) => row.topicName,
    },
    {
      key: "attempted",
      header: t("interviewTopics.colAttempted"),
      width: "40%",
      render: (row) => (
        <div className="flex items-center gap-2">
          <ProgressBar
            value={row.questionsAttempted}
            max={Math.max(row.totalQuestions, 1)}
            label={t("interviewTopics.colAttempted")}
            fill="var(--color-primary)"
            height={6}
            className="flex-1"
          />
          <span className="font-mono text-xs whitespace-nowrap text-[var(--color-text-muted)]">
            {row.questionsAttempted} / {row.totalQuestions}
          </span>
        </div>
      ),
    },
    {
      key: "lastPracticedAt",
      header: t("interviewTopics.colLast"),
      align: "right",
      render: (row) =>
        row.lastPracticedAt ? formatDateVN(row.lastPracticedAt) : "-",
    },
  ];

  return (
    <Card title={t("interviewTopics.title")}>
      <DataTable
        caption={t("interviewTopics.title")}
        columns={columns}
        rows={query.data ?? []}
        rowKey={(row) => row.topicId}
        status={query.isLoading ? "loading" : query.isError ? "error" : "ready"}
        emptyMessage={t("emptyGeneric")}
        errorMessage={t("errorGeneric")}
        loadingRowCount={4}
        minWidth={420}
      />
    </Card>
  );
}
