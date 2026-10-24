// PROTOTYPE - no RD/BD/DD yet (owner 2026-10-23: build first, docs later). See 06-plan/PROTOTYPE_DEBT.md.
//
// Learner-side class screen: join by invite code (F1-23, 01-rd/req/F1_identity.md:133-137) and, once in a
// class, its schedule, instructor and assigned problems (F2-12 also lists them on problem_list).
"use client";

import { useState, type FormEvent } from "react";
import {
  formatClassSchedule,
  useJoinClass,
  useMyClasses,
  type MyClass,
} from "@/entities/class";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import {
  Button,
  Card,
  EmptyState,
  QueryError,
  ProgressBar,
  TextField,
  SeeAllLink,
} from "@/shared/ui";

// Server codes are 8 characters (02-bd/database/F1_identity.md mục 1.9); the cap leaves slack for pasted spaces.
const CODE_MAX_LENGTH = 16;

export function MyClassesView() {
  const t = useT("myClasses");
  const tm = useT("classManagement");
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
      onError: () => toast.error(t("join.invalid")),
    });
  }

  const dayLabel = (day: number) => tm(`schedule.dayShort.${day}`);
  const list = classes.data ?? [];

  return (
    <div className="p-6">
      <div className="mb-4">
        <h1 className="text-lg font-bold tracking-tight">{t("title")}</h1>
        <p className="text-[12.5px] text-[var(--color-text-muted)]">
          {t("subtitle")}
        </p>
      </div>

      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_320px]">
        <div className="grid min-w-0 gap-4">
          {classes.isLoading ? (
            <EmptyState>{t("loading")}</EmptyState>
          ) : classes.isError ? (
            <QueryError query={classes} />
          ) : list.length === 0 ? (
            <Card className="px-[18px] py-4">
              <EmptyState>{t("empty")}</EmptyState>
            </Card>
          ) : (
            list.map((klass) => (
              <ClassCard
                key={klass.id}
                klass={klass}
                schedule={formatClassSchedule(klass.schedule, dayLabel)}
              />
            ))
          )}
        </div>

        <Card className="h-fit px-[18px] py-4">
          <h2 className="text-sm font-semibold">{t("join.title")}</h2>
          <p className="mb-3 mt-1 text-[12.5px] text-[var(--color-text-muted)]">
            {t("join.hint")}
          </p>
          <form onSubmit={submitCode} className="grid gap-3">
            <TextField
              label={t("join.label")}
              value={code}
              maxLength={CODE_MAX_LENGTH}
              placeholder="K7P2M9QX"
              autoComplete="off"
              onChange={(event) => setCode(event.target.value)}
            />
            <Button type="submit" disabled={join.isPending}>
              {t("join.submit")}
            </Button>
          </form>
        </Card>
      </div>
    </div>
  );
}

function ClassCard({ klass, schedule }: { klass: MyClass; schedule: string }) {
  const t = useT("myClasses");
  const solved = klass.assignments.filter((a) => a.solved).length;
  const total = klass.assignments.length;
  const progress = t("card.progress", { solved, total });

  return (
    <Card className="min-w-0 px-[18px] py-4">
      <h2 className="text-base font-semibold">{klass.name}</h2>
      {klass.description ? (
        <p className="mt-1 text-[12.5px] text-[var(--color-text-muted)]">
          {klass.description}
        </p>
      ) : null}
      <dl className="mt-3 grid gap-1 text-[12.5px] sm:grid-cols-[110px_1fr]">
        <dt className="text-[var(--color-text-muted)]">{t("card.instructor")}</dt>
        <dd>{klass.instructorName}</dd>
        {schedule ? (
          <>
            <dt className="text-[var(--color-text-muted)]">
              {t("card.schedule")}
            </dt>
            <dd>{schedule}</dd>
          </>
        ) : null}
        <dt className="text-[var(--color-text-muted)]">{t("card.joined")}</dt>
        <dd>{klass.joinedAtLabel}</dd>
      </dl>

      {total > 0 ? (
        <div className="mt-4">
          <ProgressBar
            value={solved}
            max={total}
            label={progress}
            height={6}
            className="mb-1.5"
          />
          <p className="text-[12px] text-[var(--color-text-muted)]">
            {progress}
          </p>
        </div>
      ) : null}
      <SeeAllLink
        href={`/classes/${klass.id}`}
        aria-label={t("card.viewDetailOf", { name: klass.name })}
      >
        {t("card.viewDetail")}
      </SeeAllLink>
    </Card>
  );
}
