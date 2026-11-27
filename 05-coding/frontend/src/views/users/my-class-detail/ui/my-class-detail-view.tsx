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
import { ArrowLeft, CalendarClock, MapPin, UserRound } from "lucide-react";
import { classScheduleParts, useMyClass } from "@/entities/class";
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
  const [expanded, setExpanded] = useState(false);
  // Real data only: the class of the mock carries its own assignments.
  const problemsQuery = useMyClassProblems(
    classId,
    page,
    Boolean(klass) && klass?.assignments === undefined,
  );

  const DoLink = ({ href, title }: { href: string; title: string }) => (
    <Link
      href={href}
      aria-label={`${t("detail.doIt")} ${title}`}
      className="inline-flex items-center rounded-full border border-[var(--color-border)] px-3 py-1 text-[12px] font-semibold hover:bg-[var(--color-surface-hover)]"
    >
      {t("detail.doIt")}
    </Link>
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
  const classProblems = problemsQuery.data;
  const totalAssigned = classProblems?.meta.totalAssigned ?? 0;
  const solvedCount = classProblems?.meta.solvedCount ?? null;
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
    {
      key: "action",
      header: <span className="sr-only">{t("detail.doIt")}</span>,
      width: "110px",
      align: "right",
      render: (a) =>
        a.solved ? null : (
          <DoLink href={`/problems/${a.problemId}`} title={a.title} />
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
      key: "level",
      header: t("detail.colLevel"),
      width: "110px",
      render: (row) => (
        <span className="text-[12.5px]">{row.levelDisplayName}</span>
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
    {
      key: "action",
      header: <span className="sr-only">{t("detail.doIt")}</span>,
      width: "110px",
      align: "right",
      render: (row) =>
        row.withdrawn || row.solveState === "solved" ? null : (
          <DoLink href={`/problems/${row.problemId}`} title={row.title} />
        ),
    },
  ];

  const parts = classScheduleParts(klass.schedule, (day) =>
    tm(`schedule.dayShort.${day}`),
  );
  const shownTotal = assignments ? total : totalAssigned;
  const shownSolved = assignments ? solved : solvedCount;
  const done =
    shownSolved !== null && shownTotal > 0 && shownSolved >= shownTotal;
  const remaining = shownSolved === null ? 0 : shownTotal - shownSolved;
  const longDescription = (klass.description?.length ?? 0) > 160;

  return (
    <div className="p-6">
      <div className="mb-4 flex items-start gap-3">
        {back}
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-lg font-bold tracking-tight">{klass.name}</h1>
            {done ? (
              <Badge variant="success">{t("card.allDone")}</Badge>
            ) : remaining > 0 ? (
              <Badge>{t("card.remaining", { count: remaining })}</Badge>
            ) : null}
          </div>
          {klass.description ? (
            <>
              <p
                className={
                  "max-w-3xl text-[12.5px] text-[var(--color-text-muted)] " +
                  (expanded ? "" : "line-clamp-2")
                }
              >
                {klass.description}
              </p>
              {longDescription ? (
                <button
                  type="button"
                  onClick={() => setExpanded((value) => !value)}
                  className="mt-0.5 text-[12px] font-semibold text-[var(--color-primary)] hover:underline"
                >
                  {t(expanded ? "detail.showLess" : "detail.showMore")}
                </button>
              ) : null}
            </>
          ) : null}
        </div>
      </div>

      <Card className="mb-4 px-[18px] py-4">
        <h2 className="sr-only">{t("detail.info")}</h2>
        <div className="grid gap-4 md:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_minmax(220px,0.8fr)]">
          <dl className="text-[12.5px]">
            <dt className="text-[var(--color-text-muted)]">
              {t("card.instructor")}
            </dt>
            <dd className="mt-0.5 flex items-center gap-1.5 text-[13.5px] font-medium">
              <UserRound size={14} className="text-[var(--color-text-muted)]" />
              {klass.instructorName}
            </dd>
            <dt className="mt-3 text-[var(--color-text-muted)]">
              {t("card.joined")}
            </dt>
            <dd className="mt-0.5 font-medium">{klass.joinedAtLabel}</dd>
          </dl>

          <dl className="text-[12.5px]">
            <dt className="text-[var(--color-text-muted)]">
              {t("card.schedule")}
            </dt>
            <dd className="mt-1">
              {parts.when || parts.room || parts.period ? (
                <span className="flex flex-wrap gap-1.5">
                  {parts.when ? (
                    <Badge>
                      <CalendarClock size={12} />
                      {parts.when}
                    </Badge>
                  ) : null}
                  {parts.room ? (
                    <Badge>
                      <MapPin size={12} />
                      {parts.room}
                    </Badge>
                  ) : null}
                  {parts.period ? <Badge>{parts.period}</Badge> : null}
                </span>
              ) : (
                <span className="text-[var(--color-text-muted)]">
                  {t("detail.noSchedule")}
                </span>
              )}
            </dd>
            {parts.note ? (
              <dd className="mt-2 text-[var(--color-text-muted)]">
                {parts.note}
              </dd>
            ) : null}
          </dl>

          <div>
            <h3 className="text-[12.5px] text-[var(--color-text-muted)]">
              {t("detail.progress")}
            </h3>
            {shownSolved === null ? (
              totalAssigned > 0 ? (
                <p className="mt-1 text-[12.5px] text-[var(--color-text-muted)]">
                  {t("detail.progressUnknown", { total: shownTotal })}
                </p>
              ) : null
            ) : (
              <>
                <p className="mt-0.5 text-[13.5px] font-medium">
                  {t("card.progress", {
                    solved: shownSolved,
                    total: shownTotal,
                  })}
                </p>
                <ProgressBar
                  value={shownSolved}
                  max={Math.max(shownTotal, 1)}
                  label={t("card.progress", {
                    solved: shownSolved,
                    total: shownTotal,
                  })}
                  height={6}
                  className="mt-2"
                />
              </>
            )}
          </div>
        </div>
      </Card>

      <Card className="min-w-0 px-[18px] py-4">
        <h2 className="mb-3 text-sm font-semibold">{t("card.assignments")}</h2>
        {assignments === undefined ? (
          problemsQuery.isError ? (
            <QueryError query={problemsQuery} />
          ) : !classProblems ? (
            <Skeleton className="h-24" />
          ) : totalAssigned === 0 ? (
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
          )
        ) : total === 0 ? (
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
    </div>
  );
}
