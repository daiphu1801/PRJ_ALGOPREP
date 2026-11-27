// F1 #25/#25A supplies membership; F2 #13A supplies whole-class progress independently per card.
//
// Learner-side class screen: join by invite code (F1-23, 01-rd/req/F1_identity.md:133-137) and, once in a
// class, its schedule, instructor and assigned problems (F2-12 also lists them on problem_list).
"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { ArrowRight, UserRound } from "lucide-react";
import { useMyClassProblems } from "@/entities/problem";
import { isMockMode } from "@/shared/api";
import {
  classScheduleParts,
  useJoinClass,
  useMyClasses,
  type MyClass,
} from "@/entities/class";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  DataTable,
  EmptyState,
  QueryError,
  ProgressBar,
  TextField,
  type DataTableColumn,
} from "@/shared/ui";

// Server codes are 8 characters (02-bd/database/F1_identity.md mục 1.9); the cap leaves slack for pasted spaces.
const CODE_MAX_LENGTH = 16;

export function MyClassesView() {
  const t = useT("myClasses");
  const tm = useT("classManagement");
  const toMessage = useApiErrorMessage();
  const classes = useMyClasses();
  const join = useJoinClass();
  const [code, setCode] = useState("");

  function submitCode(event: FormEvent) {
    event.preventDefault();
    if (!code.trim()) return toast.error(t("join.empty"));
    join.mutate(code, {
      onSuccess: (klass) => {
        setCode("");
        toast.success(t("join.success", { name: klass.name }));
      },
      // IDT-410 (wrong or expired code) and IDT-412 (too many wrong codes) read by their own code.
      onError: (error) => toast.error(toMessage(error)),
    });
  }

  const dayLabel = (day: number) => tm(`schedule.dayShort.${day}`);
  const list = classes.data ?? [];

  const columns: DataTableColumn<MyClass>[] = [
    {
      key: "name",
      width: "28%",
      header: t("card.className"),
      render: (klass) => (
        <Link
          href={`/classes/${klass.id}`}
          aria-label={t("card.viewDetailOf", { name: klass.name })}
          className="block truncate font-semibold hover:underline"
          title={klass.name}
        >
          {klass.name}
        </Link>
      ),
    },
    {
      key: "instructor",
      header: t("card.instructor"),
      width: "17%",
      render: (klass) => (
        <span className="flex items-center gap-1.5">
          <UserRound
            size={14}
            className="shrink-0 text-[var(--color-text-muted)]"
          />
          <span className="truncate">{klass.instructorName}</span>
        </span>
      ),
    },
    {
      key: "schedule",
      header: t("card.schedule"),
      width: "20%",
      render: (klass) => {
        const parts = classScheduleParts(klass.schedule, dayLabel);
        return (
          <span className="block text-[12.5px]">
            <span className="block">{parts.when}</span>
            {parts.room ? (
              <span
                className="block truncate text-[var(--color-text-muted)]"
                title={parts.room}
              >
                {parts.room}
              </span>
            ) : null}
          </span>
        );
      },
    },
    {
      key: "progress",
      header: t("detail.progress"),
      width: "24%",
      render: (klass) => <ClassProgressCell klass={klass} />,
    },
    {
      key: "status",
      header: t("card.status"),
      width: "11%",
      render: (klass) => <ClassStatusCell klass={klass} />,
    },
    {
      key: "open",
      header: <span className="sr-only">{t("card.viewDetail")}</span>,
      width: "48px",
      align: "right",
      render: (klass) => (
        <Link
          href={`/classes/${klass.id}`}
          tabIndex={-1}
          aria-hidden
          className="inline-flex text-[var(--color-text-muted)]"
        >
          <ArrowRight size={16} />
        </Link>
      ),
    },
  ];

  return (
    <div className="p-6">
      <div className="mb-4">
        <h1 className="text-lg font-bold tracking-tight">{t("title")}</h1>
        <p className="text-[12.5px] text-[var(--color-text-muted)]">
          {t("subtitle")}
        </p>
      </div>

      <Card className="mb-4 px-[18px] py-3">
        <form
          onSubmit={submitCode}
          className="flex flex-wrap items-center gap-3"
        >
          <div className="min-w-[200px] flex-1">
            <h2 className="text-sm font-semibold">{t("join.title")}</h2>
            <p className="text-[12.5px] text-[var(--color-text-muted)]">
              {t("join.hint")}
            </p>
          </div>
          <div className="w-full sm:w-[220px]">
            <TextField
              label={t("join.label")}
              hideLabel
              value={code}
              maxLength={CODE_MAX_LENGTH}
              placeholder="K7P2M9QX"
              autoComplete="off"
              onChange={(event) => setCode(event.target.value)}
            />
          </div>
          <Button type="submit" disabled={join.isPending}>
            {t("join.submit")}
          </Button>
        </form>
      </Card>

      {classes.isLoading ? (
        <EmptyState>{t("loading")}</EmptyState>
      ) : classes.isError ? (
        <QueryError query={classes} />
      ) : list.length === 0 ? (
        <Card className="px-[18px] py-4">
          <EmptyState>{t("empty")}</EmptyState>
        </Card>
      ) : (
        <Card className="min-h-[calc(calc(100*var(--dvh1))-25rem)] min-w-0 px-[18px] py-3">
          <DataTable
            caption={t("title")}
            columns={columns}
            rows={list}
            rowKey={(klass) => klass.id}
            emptyMessage={t("empty")}
            minWidth={860}
          />
        </Card>
      )}
    </div>
  );
}

// One first-page read per class; meta covers every page, never just the returned 20 rows. The status
// and the progress cells both call this: react-query shares the one request.
function useClassProgress(klass: MyClass) {
  const real = !isMockMode();
  const assignments = useMyClassProblems(klass.id, 1, real);
  const total = real
    ? (assignments.data?.meta?.totalAssigned ?? null)
    : (klass.assignments?.length ?? 0);
  const solved = real
    ? (assignments.data?.meta?.solvedCount ?? null)
    : (klass.assignments ?? []).filter((a) => a.solved).length;
  return { assignments, real, total, solved };
}

function ClassStatusCell({ klass }: { klass: MyClass }) {
  const t = useT("myClasses");
  const { total, solved } = useClassProgress(klass);
  if (solved === null || total === null || total === 0) return null;
  return solved >= total ? (
    <Badge variant="success">{t("card.allDone")}</Badge>
  ) : (
    <Badge>{t("card.remaining", { count: total - solved })}</Badge>
  );
}

function ClassProgressCell({ klass }: { klass: MyClass }) {
  const t = useT("myClasses");
  const { assignments, real, total, solved } = useClassProgress(klass);
  if (real && assignments.isError) return <QueryError query={assignments} />;
  if (real && assignments.isLoading) {
    return (
      <span className="text-[12px] text-[var(--color-text-muted)]">
        {t("card.progressLoading")}
      </span>
    );
  }
  if (total === null || total === 0) return null;
  const progress =
    solved === null
      ? t("card.assignedCount", { total })
      : t("card.progress", { solved, total });
  return (
    <div className="flex items-center gap-3">
      {solved !== null ? (
        <ProgressBar
          value={solved}
          max={total}
          label={progress}
          height={4}
          className="min-w-0 flex-1"
        />
      ) : null}
      <span className="shrink-0 text-[12px] text-[var(--color-text-muted)]">
        {progress}
      </span>
    </div>
  );
}
