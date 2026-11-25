// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import Link from "next/link";
import {
  problemLevelLabel,
  problemLevelTone,
  useProblemLevels,
  useSuggestedProblems,
  type CatalogRow,
} from "@/entities/problem";
import { useT } from "@/shared/i18n";
import {
  Badge,
  DataTable,
  SeeAllLink,
  type DataTableColumn,
} from "@/shared/ui";

/** Only what is still to do, and few enough to read at a glance (DEC-2026-1123-student-dashboard-trim). */
const SHOWN = 5;

/**
 * 09-layoutBase/Dashboard AlgoPrep.dc.html:217-266 — "Bài toán gợi ý": a 4-column table showing status dot,
 * id, title, `function` tag, topic, difficulty, AC rate. The prototype's difficulty tabs and topic chips were
 * dropped on 2026-11-23 (DEC-2026-1123-student-dashboard-trim): filtering belongs to the problem list, which
 * "Xem tất cả" opens, and solved problems are no longer suggested.
 *
 * The server ranks and filters (F2 #12, 03-dd/api/F2_problem-bank.md section 13, USR0601 BR-SC-07):
 * unsolved first, then the topics where the learner scores lowest, at most seven rows. A filter change
 * asks again, and the previous rows stay on screen while it loads.
 */
export function SuggestedProblemsBlock() {
  const t = useT("dashboard");
  const levels = useProblemLevels();
  const suggestions = useSuggestedProblems("all", "all");
  // The server puts unsolved first; a solved one is not a suggestion, so it is left out here.
  const rows = (suggestions.data ?? [])
    .filter((problem) => problem.solveState !== "solved")
    .slice(0, SHOWN);

  const solveDot = (problem: CatalogRow) => (
    <span
      role={problem.solveState ? "img" : undefined}
      aria-hidden={problem.solveState ? undefined : true}
      aria-label={
        problem.solveState ? t(`solveState.${problem.solveState}`) : undefined
      }
      className="inline-block h-2 w-2 shrink-0 rounded-full"
      style={{
        background: `var(${
          problem.solveState === "solved"
            ? "--color-success"
            : problem.solveState === "attempted"
              ? "--color-admin-warn"
              : "--color-border"
        })`,
      }}
    />
  );

  const columns: DataTableColumn<CatalogRow>[] = [
    {
      key: "title",
      header: t("suggested.colProblem"),
      render: (problem) => (
        <div className="flex min-w-0 items-center gap-2.5">
          {solveDot(problem)}
          <span className="shrink-0 font-mono text-[11.5px] text-[var(--color-text-subtle)]">
            {problem.code}
          </span>
          <Link
            href={`/problems/${problem.id}`}
            className="truncate font-medium hover:underline"
          >
            {problem.title}
          </Link>
          {problem.submissionModel !== "stdioOnly" && (
            <Badge variant="cyan">{t("suggested.tagFunction")}</Badge>
          )}
        </div>
      ),
    },
    {
      key: "topics",
      header: t("suggested.colTopic"),
      width: "120px",
      render: (problem) => (
        <span className="block truncate text-[13px] text-[var(--color-text-muted)]">
          {problem.topics[0]}
        </span>
      ),
    },
    {
      key: "difficulty",
      header: t("suggested.colDifficulty"),
      width: "104px",
      render: (problem) => (
        <Badge variant={problemLevelTone(levels, problem.difficulty)}>
          {problemLevelLabel(levels, problem.difficulty)}
        </Badge>
      ),
    },
    {
      key: "acRate",
      header: t("suggested.colAcRate"),
      width: "84px",
      align: "right",
      render: (problem) => (
        <span className="font-mono text-[13px] text-[var(--color-text-muted)]">
          {problem.acRate === null ? "-" : `${problem.acRate}%`}
        </span>
      ),
    },
  ];

  return (
    <section aria-label={t("suggested.title")} className="glass-card p-3">
      <div className="mb-3 flex items-baseline justify-between gap-3">
        <h2 className="text-sm font-semibold">{t("suggested.title")}</h2>
        <SeeAllLink href="/problems">{t("seeAll")}</SeeAllLink>
      </div>

      {/* A phone cannot hold four columns: the table scrolled sideways and cut the topic off with no sign
          that more was there. Below `md` each problem is a card (title line, then topic, level, AC rate);
          from `md` up the table is unchanged. Both are in the page, CSS picks one. */}
      <div className="hidden md:block">
        <DataTable
          caption={t("suggested.title")}
          columns={columns}
          rows={rows}
          rowKey={(problem) => problem.id}
          emptyMessage={t("suggested.allSolved")}
          minWidth={520}
        />
      </div>
      <ul className="flex flex-col gap-2 md:hidden">
        {rows.length === 0 ? (
          <li className="py-6 text-center text-[13px] text-[var(--color-text-muted)]">
            {t("suggested.allSolved")}
          </li>
        ) : (
          rows.map((problem) => (
            <li
              key={problem.id}
              className="rounded-lg border border-[var(--color-border)] p-3"
            >
              <div className="flex items-start gap-2.5">
                <span className="mt-[7px]">{solveDot(problem)}</span>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
                    <span className="font-mono text-[11.5px] text-[var(--color-text-subtle)]">
                      {problem.code}
                    </span>
                    <Link
                      href={`/problems/${problem.id}`}
                      className="font-medium hover:underline"
                    >
                      {problem.title}
                    </Link>
                    {problem.submissionModel !== "stdioOnly" && (
                      <Badge variant="cyan">{t("suggested.tagFunction")}</Badge>
                    )}
                  </div>
                  <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1.5 text-[12.5px] text-[var(--color-text-muted)]">
                    <span>{problem.topics[0]}</span>
                    <Badge
                      variant={problemLevelTone(levels, problem.difficulty)}
                    >
                      {problemLevelLabel(levels, problem.difficulty)}
                    </Badge>
                    <span className="ml-auto font-mono">
                      {t("suggested.colAcRate")}{" "}
                      {problem.acRate === null ? "-" : `${problem.acRate}%`}
                    </span>
                  </div>
                </div>
              </div>
            </li>
          ))
        )}
      </ul>
    </section>
  );
}
