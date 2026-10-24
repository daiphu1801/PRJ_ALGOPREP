// PROTOTYPE - no RD/BD/DD yet (owner 2026-10-23). See 06-plan/PROTOTYPE_DEBT.md mục 40.
//
// Learner-side class detail: everything about one class the learner belongs to. Read-only; the
// learner cannot edit the class or leave it from here (no RD requirement for leaving).
"use client";

import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { formatClassSchedule, useMyClass } from "@/entities/class";
import { useT } from "@/shared/i18n";
import {
  Badge,
  Card,
  DataTable,
  EmptyState,
  IconAction,
  ProgressBar,
  QueryError,
  Skeleton,
  type DataTableColumn,
} from "@/shared/ui";
import type { MyClassAssignment } from "@/entities/class";

export function MyClassDetailView({ classId }: { classId: string }) {
  const t = useT("myClasses");
  const tm = useT("classManagement");
  const classQuery = useMyClass(classId);
  const { data: klass, isLoading } = classQuery;

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

  const solved = klass.assignments.filter((a) => a.solved).length;
  const total = klass.assignments.length;
  const progress = t("card.progress", { solved, total });
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
        </div>

        <Card className="min-w-0 px-[18px] py-4">
          <h2 className="mb-3 text-sm font-semibold">{t("card.assignments")}</h2>
          {total === 0 ? (
            <EmptyState>{t("card.noAssignments")}</EmptyState>
          ) : (
            <DataTable
              columns={columns}
              rows={klass.assignments}
              rowKey={(a) => a.problemId}
              caption={t("card.assignments")}
              emptyMessage={t("card.noAssignments")}
            />
          )}
        </Card>
      </div>
    </div>
  );
}
