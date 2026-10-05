// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import {
  fetchProblemListPage,
  problemLevelLabel,
  problemLevelTone,
  useProblemLevels,
  type ProblemListItem,
} from "@/entities/problem";
import { useTopicProgress } from "@/entities/progress";
import { deriveSkillRadar } from "../../model/derive-skill-radar";
import { useT } from "@/shared/i18n";
import { Badge, DataTable, SegmentedTabs, type DataTableColumn } from "@/shared/ui";

/**
 * 09-layoutBase/Dashboard AlgoPrep.dc.html:217-266 — "Bài toán gợi ý": difficulty tabs, topic
 * chips, and a 4-column table showing status dot, id, title, `function` tag, topic, difficulty,
 * AC rate.
 *
 * Reads `entities/problem` rather than owning a list: these are rows from the same catalogue
 * `problem_list` renders, filtered and re-ordered. A dashboard-owned copy would be a second set of
 * problem titles to keep in step with the first.
 *
 * Ranking rule (01-rd/screens/users/USR0601_dashboard.md mục 4 Q2 — plain rule, no AI call):
 * unsolved problems first, then problems whose topic scores lowest on the skill radar. That ties
 * the suggestion to the same weakness the greeting and radar call out, instead of being a third
 * independent opinion about what the learner is bad at.
 */
const SUGGESTION_COUNT = 7;

type DifficultyFilter = string;

export function SuggestedProblemsBlock() {
  const t = useT("dashboard");
  const levels = useProblemLevels();
  const [difficulty, setDifficulty] = useState<DifficultyFilter>("all");
  const [topicId, setTopicId] = useState<string>("all");
  const topicsQuery = useTopicProgress("all");

  // Frozen on first render like views/problem-list does: the mock returns fresh objects every
  // call, so calling it inline would give `useMemo` a new `page.items` identity each render and
  // re-sort the whole catalogue for nothing.
  const [page] = useState(fetchProblemListPage);
  const weakestFirst = useMemo(() => {
    const radar = topicsQuery.data ? deriveSkillRadar(topicsQuery.data) : [];
    const scoreByTopic = new Map(radar.map((point) => [point.topicName.toLowerCase(), point.score]));
    // Topics with no radar score sort as 100 — "nothing known against them", so they rank below a
    // topic the learner is measurably weak at, rather than above everything on a missing value.
    const weaknessOf = (problem: ProblemListItem) =>
      Math.min(...problem.topics.map((name) => scoreByTopic.get(name.toLowerCase()) ?? 100), 100);

    return [...page.items].sort((left, right) => {
      const leftUnsolved = left.solveState === "solved" ? 1 : 0;
      const rightUnsolved = right.solveState === "solved" ? 1 : 0;
      if (leftUnsolved !== rightUnsolved) return leftUnsolved - rightUnsolved;
      return weaknessOf(left) - weaknessOf(right);
    });
  }, [page.items, topicsQuery.data]);

  const rows = weakestFirst
    .filter(
      (problem) =>
        (difficulty === "all" || problem.difficulty === difficulty) &&
        (topicId === "all" || problem.topics.some((name) => name.toLowerCase() === topicId)),
    )
    .slice(0, SUGGESTION_COUNT);

  const columns: DataTableColumn<ProblemListItem>[] = [
    {
      key: "title",
      header: t("suggested.colProblem"),
      render: (problem) => (
        <div className="flex min-w-0 items-center gap-2.5">
          <span
            aria-label={t(`solveState.${problem.solveState}`)}
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
          <span className="shrink-0 font-mono text-[11.5px] text-[var(--color-text-subtle)]">{problem.code}</span>
          <Link href={`/problems/${problem.id}`} className="truncate font-medium hover:underline">
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
        <span className="block truncate text-[13px] text-[var(--color-text-muted)]">{problem.topics[0]}</span>
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
        <h3 className="text-sm font-semibold">{t("suggested.title")}</h3>
        <span className="font-mono text-xs text-[var(--color-text-subtle)]">
          {t("suggested.count", { count: rows.length })}
        </span>
      </div>

      <div className="mb-3 flex flex-wrap items-center gap-2.5">
        <SegmentedTabs
          label={t("suggested.difficultyLabel")}
          value={difficulty}
          onValueChange={setDifficulty}
          options={[
            { value: "all", label: t("filterAll") },
                        ...levels.map((level) => ({ value: level.key, label: level.label })),
          ]}
        />
        <div className="flex flex-wrap gap-1.5" role="group" aria-label={t("suggested.topicLabel")}>
          {[{ id: "all", name: t("filterAll") }, ...page.topics].map((topic) => (
            <button
              key={topic.id}
              type="button"
              aria-pressed={topicId === topic.id}
              onClick={() => setTopicId(topic.id)}
              className={`shrink-0 rounded-full border px-2.5 py-1 text-[12.5px] font-medium whitespace-nowrap ${
                topicId === topic.id
                  ? "border-[var(--color-primary)] text-[var(--color-primary)]"
                  : "border-[var(--color-border)] text-[var(--color-text-muted)]"
              }`}
            >
              {topic.name}
            </button>
          ))}
        </div>
      </div>

      <DataTable
        caption={t("suggested.title")}
        columns={columns}
        rows={rows}
        rowKey={(problem) => problem.id}
        emptyMessage={t("suggested.empty")}
        minWidth={520}
      />
    </section>
  );
}
