// PROTOTYPE with a real API behind it (task 12, 03-dd/api/F1_identity.md #25B); see
// 06-plan/PROTOTYPE_DEBT.md mục 40. The class itself is F1. The assigned problems and the progress
// come from F2 #13A (`useMyClassProblems`, real data only): a withdrawn problem keeps its row but is
// not a link, and an unknown solved count shows the total alone, never an invented ratio. On mock
// data the class carries its own assignments and the old blocks are drawn from them.
//
// Learner-side class detail: everything about one class the learner belongs to. Read-only; the
// learner cannot edit the class or leave it from here (no RD requirement for leaving).
"use client";

import { useState } from "react";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { formatClassSchedule, useMyClass } from "@/entities/class";
import { useMyClassProblems, type ClassProblemRow } from "@/entities/problem";
import { useLocale, useT } from "@/shared/i18n";
import { formatDateVN } from "@/shared/lib";
import {
  Badge,
  Card,
  DataTable,
  EmptyState,
  IconAction,
  ProgressBar,
  QueryError,
  Pagination,
  Skeleton,
  type DataTableColumn,
} from "@/shared/ui";
import type { MyClassAssignment } from "@/entities/class";

export function MyClassDetailView({ classId }: { classId: string }) {
  const t = useT("myClasses");
  const tm = useT("classManagement");
  const locale = useLocale() === "en" ? "en" : "vi";
  const classQuery = useMyClass(classId);
  const { data: klass, isLoading } = classQuery;
  const [page, setPage] = useState(1);
  // Real data only: the class of the mock carries its own assignments.
  const problemsQuery = useMyClassProblems(
    classId,
    page,
    Boolean(klass) && klass?.assignments === undefined,
  );

  const back = (
    <IconAction icon={ArrowLeft} label={t("detail.back")} href="/classes" />
  );

  if (isLoading) {
    return (
      <div className="p-6">
        <Skeleton className="h-40" />
      </div>
    );
  }
  if (classQuery.isError) {
    return (
      <div className="p-6">
        <QueryError query={classQuery} />
      </div>
    );
  }
  if (!klass) {
    return (
      <div className="p-6">
        <div className="mb-3">{back}</div>
        <Card className="px-[18px] py-4">
          <EmptyState>{t("detail.notFound")}</EmptyState>
        </Card>
      </div>
    );
  }

  const assignments = klass.assignments;
  const solved = (assignments ?? []).filter((a) => a.solved).length;
  const total = assignments?.length ?? 0;
  const progress = t("card.progress", { solved, total });
  const classProblems = problemsQuery.data;
  const totalAssigned = classProblems?.meta.totalAssigned ?? 0;
  const solvedCount = classProblems?.meta.solvedCount ?? null;
  const schedule = formatClassSchedule(klass.schedule, (day) =>
    tm(`schedule.dayShort.${day}`),
  );

  const columns: DataTableColumn<MyClassAssignment>[] = [
    {
      key: "title",
      header: t("detail.colProblem"),
      render: (a) => (
        <Link
          href={`/problems/${a.problemId}`}
          className="block truncate font-semibold hover:underline"
        >
          {a.title}
        </Link>
      ),
    },
    {
      key: "assignedAt",
      header: t("detail.colAssignedAt"),
      width: "130px",
      render: (a) => (
        <span className="text-[12.5px] text-[var(--color-text-muted)]">
          {a.assignedAtLabel}
        </span>
      ),
    },
    {
      key: "state",
      header: t("detail.colState"),
      width: "110px",
      render: (a) => (
        <Badge variant={a.solved ? "success" : "neutral"}>
          {t(a.solved ? "card.solved" : "card.todo")}
        </Badge>
      ),
    },
  ];

  const stateKey = {
    solved: "card.solved",
    attempted: "card.attempted",
    todo: "card.todo",
  } as const;
  const problemColumns: DataTableColumn<ClassProblemRow>[] = [
    {
      key: "title",
      header: t("detail.colProblem"),
      render: (row) =>
        row.withdrawn ? (
          <span className="flex items-center gap-2">
            <span className="truncate text-[var(--color-text-muted)]">
              {row.title}
            </span>
            <Badge variant="warn">{t("detail.withdrawn")}</Badge>
          </span>
        ) : (
          <Link
            href={`/problems/${row.problemId}`}
            className="block truncate font-semibold hover:underline"
          >
            {row.title}
          </Link>
        ),
    },
    {
      key: "assignedAt",
      header: t("detail.colAssignedAt"),
      width: "130px",
      render: (row) => (
        <span className="text-[12.5px] text-[var(--color-text-muted)]">
          {formatDateVN(row.assignedAt, { locale })}
        </span>
      ),
    },
    {
      key: "state",
      header: t("detail.colState"),
      width: "110px",
      render: (row) => (
        <Badge variant={row.solveState === "solved" ? "success" : "neutral"}>
          {t(stateKey[row.solveState])}
        </Badge>
      ),
    },
  ];

  const info: [string, string][] = [
    [t("card.instructor"), klass.instructorName],
    [t("card.schedule"), schedule || t("detail.noSchedule")],
    [t("card.joined"), klass.joinedAtLabel],
  ];

  return (
    <div className="p-6">
      <div className="mb-4 flex items-start gap-3">
        {back}
        <div className="min-w-0">
          <h1 className="text-lg font-bold tracking-tight">{klass.name}</h1>
          {klass.description ? (
            <p className="text-[12.5px] text-[var(--color-text-muted)]">
              {klass.description}
            </p>
          ) : null}
        </div>
      </div>

      <div className="grid gap-4 lg:grid-cols-[320px_minmax(0,1fr)]">
        <div className="grid h-fit gap-4">
          <Card className="px-[18px] py-4">
            <h2 className="mb-2 text-sm font-semibold">{t("detail.info")}</h2>
            <dl className="grid gap-2 text-[12.5px]">
              {info.map(([label, value]) => (
                <div key={label}>
                  <dt className="text-[var(--color-text-muted)]">{label}</dt>
                  <dd className="font-medium">{value}</dd>
                </div>
              ))}
            </dl>
          </Card>
          {assignments ? (
            <Card className="px-[18px] py-4">
              <h2 className="mb-2 text-sm font-semibold">
                {t("detail.progress")}
              </h2>
              <ProgressBar
                value={solved}
                max={Math.max(total, 1)}
                label={progress}
                height={8}
                className="mb-1.5"
              />
              <p className="text-[12.5px] text-[var(--color-text-muted)]">
                {progress}
              </p>
            </Card>
          ) : null}
        </div>

        {assignments === undefined ? (
          <Card className="min-w-0 px-[18px] py-4">
            <h2 className="mb-3 text-sm font-semibold">
              {t("card.assignments")}
            </h2>
            {problemsQuery.isError ? (
              <QueryError query={problemsQuery} />
            ) : !classProblems ? (
              <Skeleton className="h-24" />
            ) : (
              <>
                {solvedCount === null ? (
                  <p className="mb-3 text-[12.5px] text-[var(--color-text-muted)]">
                    {t("detail.progressUnknown", { total: totalAssigned })}
                  </p>
                ) : (
                  <div className="mb-3">
                    <ProgressBar
                      value={solvedCount}
                      max={Math.max(totalAssigned, 1)}
                      label={t("card.progress", {
                        solved: solvedCount,
                        total: totalAssigned,
                      })}
                      height={8}
                      className="mb-1.5"
                    />
                    <p className="text-[12.5px] text-[var(--color-text-muted)]">
                      {t("card.progress", {
                        solved: solvedCount,
                        total: totalAssigned,
                      })}
                    </p>
                  </div>
                )}
                {totalAssigned === 0 ? (
                  <EmptyState>{t("card.noAssignments")}</EmptyState>
                ) : (
                  <>
                    <DataTable
                      columns={problemColumns}
                      rows={classProblems.items}
                      rowKey={(row) => row.problemId}
                      caption={t("card.assignments")}
                      emptyMessage={t("card.noAssignments")}
                    />
                    {classProblems.page.totalPages > 1 ? (
                      <Pagination
                        page={classProblems.page.number}
                        pageSize={classProblems.page.size}
                        total={classProblems.page.totalItems}
                        onPageChange={setPage}
                        summary={t("detail.pageSummary", {
                          page: classProblems.page.number,
                          total: classProblems.page.totalPages,
                        })}
                        previousLabel={t("detail.previous")}
                        nextLabel={t("detail.next")}
                      />
                    ) : null}
                  </>
                )}
              </>
            )}
          </Card>
        ) : (
          <Card className="min-w-0 px-[18px] py-4">
            <h2 className="mb-3 text-sm font-semibold">
              {t("card.assignments")}
            </h2>
            {total === 0 ? (
              <EmptyState>{t("card.noAssignments")}</EmptyState>
            ) : (
              <DataTable
                columns={columns}
                rows={assignments}
                rowKey={(a) => a.problemId}
                caption={t("card.assignments")}
                emptyMessage={t("card.noAssignments")}
              />
            )}
          </Card>
        )}
      </div>
    </div>
  );
}
