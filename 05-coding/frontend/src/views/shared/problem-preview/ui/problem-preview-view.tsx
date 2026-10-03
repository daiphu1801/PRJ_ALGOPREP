// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 21.
//
// "Xem như người học" (BD SHR0202 Q6, Q13): opened in a NEW browser tab from the authoring screen and
// shows the LAST SAVED copy of the problem, never unsaved edits. Read-only statement, constraints,
// examples and limits only — what a learner sees, so no testcases, reference solution or AI settings.
//
// A real learner-view render (USR0102 `problem_detail`) needs a preview mode on that screen; until
// then this is a plain read-only page inside the area shell. Saved data comes from the mock's
// localStorage copy, so only a save made in this browser shows up.
"use client";

import { useProblemDraft } from "@/entities/problem";
import { useT } from "@/shared/i18n";
import { Badge, Card, ErrorState, MarkdownPreview, NoticeTile, Skeleton } from "@/shared/ui";

const DIFFICULTY_VARIANT = { easy: "success", medium: "warn", hard: "negative" } as const;

type Props = {
  /** Route param, e.g. "121". */
  problemId: string;
};

export function ProblemPreviewView({ problemId }: Props) {
  const t = useT("problemPreview");
  const ta = useT("problemAuthoring");
  const query = useProblemDraft(problemId);

  if (query.isError) return <ErrorState>{t("loadFailed")}</ErrorState>;
  if (!query.data) return <Skeleton className="mx-auto h-[320px] max-w-3xl" aria-busy="true" />;
  const { draft, savedAt } = query.data;

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-3.5">
      <NoticeTile tone="info" title={ta("previewAsLearner")}>
        {savedAt
          ? t("savedAt", { time: new Date(savedAt).toLocaleString() })
          : t("neverSaved")}
      </NoticeTile>

      <Card
        title={draft.title}
        action={
          <Badge variant={DIFFICULTY_VARIANT[draft.difficulty]}>{ta(`difficulty.${draft.difficulty}`)}</Badge>
        }
      >
        <h2 className="sr-only">{t("statementTitle")}</h2>
        <MarkdownPreview>{draft.body}</MarkdownPreview>

        <p className="mt-4 mb-1.5 text-[11px] font-semibold tracking-[0.07em] text-[var(--color-text-subtle)] uppercase">
          {t("constraintsLabel")}
        </p>
        <pre className="font-mono text-[12.5px] whitespace-pre-wrap">{draft.constraints}</pre>

        <p className="mt-4 mb-1.5 text-[11px] font-semibold tracking-[0.07em] text-[var(--color-text-subtle)] uppercase">
          {t("limitsTitle")}
        </p>
        <p className="text-[12.5px] text-[var(--color-text-muted)]">
          {t("timeLimit")}: {draft.limits.timeLimitMs} ms · {t("memoryLimit")}: {draft.limits.memoryLimitMb} MB
        </p>
      </Card>

      <Card title={t("examplesTitle")}>
        <ol className="flex flex-col gap-2.5">
          {draft.examples.map((example) => (
            <li key={example.id} className="glass-surface rounded-2xl border border-[var(--color-border)] px-3.5 py-3">
              <dl className="flex flex-col gap-1.5 text-[12.5px]">
                <div className="flex gap-2">
                  <dt className="w-20 shrink-0 text-[var(--color-text-subtle)]">{t("exampleInput")}</dt>
                  <dd className="min-w-0 font-mono">{example.input}</dd>
                </div>
                <div className="flex gap-2">
                  <dt className="w-20 shrink-0 text-[var(--color-text-subtle)]">{t("exampleOutput")}</dt>
                  <dd className="min-w-0 font-mono">{example.output}</dd>
                </div>
                {example.explanation ? (
                  <div className="flex gap-2">
                    <dt className="w-20 shrink-0 text-[var(--color-text-subtle)]">{t("exampleExplanation")}</dt>
                    <dd className="min-w-0 text-[var(--color-text-muted)]">{example.explanation}</dd>
                  </div>
                ) : null}
              </dl>
            </li>
          ))}
        </ol>
      </Card>
    </div>
  );
}
