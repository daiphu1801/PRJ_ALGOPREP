// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Read-only detail page for one problem, split out of the authoring form: `/admin/problems/[id]`
// shows this, `/admin/problems/[id]/edit` shows ProblemAuthoringView unchanged. There is no
// mockup in 09-layoutBase for it — layout is a header, then a wide column of content and a narrow
// column of properties, reusing the primitives the authoring screen already uses. The four summary
// numbers sit at the top of the narrow column (not in a row above the statement) so the statement
// starts at the top of the viewport.
//
// Data: `useProblemDraft(id)` loads the last saved copy; an id never saved here shows the sample
// problem (the mock ignores the id). Acceptance rate and
// submission count are deliberately absent — ProblemDraft has neither and inventing them would be
// fake data.
//
// The statement renders through MarkdownPreview (Markdown + LaTeX); the raw Markdown only appears
// in the authoring form.
"use client";

import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import {
  AI_GUARD_KEYS,
  SPEC_LANGUAGES,
  TESTCASE_CATEGORIES,
  exceedsSchema,
  renderSignature,
  useProblemDraft,
  problemLevelLabel,
  problemLevelTone,
  problemTopicLabel,
  useProblemLevels,
  useProblemTopics,
  type AiGuardKey,
  type ProblemLimits,
  type TestcaseCategory,
} from "@/entities/problem";
import { isNotFound } from "@/shared/api";
import { useT } from "@/shared/i18n";
import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorState,
  IconAction,
  MarkdownPreview,
  PageHeader,
  Skeleton,
} from "@/shared/ui";

const LIMIT_KEYS: (keyof ProblemLimits)[] = [
  "timeLimitMs",
  "memoryLimitMb",
  "outputLimitKb",
  "stackLimitMb",
];

type Props = {
  /** Route param, e.g. "121" — the list links with the code minus its "#". */
  problemId: string;
  /** Area root this screen is mounted under, e.g. "/admin/problems". */
  basePath: string;
};

export function ProblemInfoView({ problemId, basePath }: Props) {
  const t = useT("problemInfo");
  const ta = useT("problemAuthoring");
  const levelList = useProblemLevels();
  const topicList = useProblemTopics();
  const query = useProblemDraft(problemId);

  if (isNotFound(query.error)) return <EmptyState>{t("notFound")}</EmptyState>;
  if (query.isError) return <ErrorState>{t("loadFailed")}</ErrorState>;
  if (!query.data) {
    return (
      <div className="flex flex-col gap-3.5" aria-busy="true">
        <Skeleton className="h-[62px] w-full" />
        <Skeleton className="h-[320px] w-full" />
      </div>
    );
  }
  const draft = query.data.draft;

  const approved = draft.testcases.filter((row) => row.approved);
  const pending = draft.testcases.length - approved.length;
  const publicCount = approved.filter(
    (row) => row.visibility === "public",
  ).length;
  const coverage = TESTCASE_CATEGORIES.map((category: TestcaseCategory) => ({
    category,
    approved: approved.filter((row) => row.category === category).length,
  }));

  return (
    <div>
      <PageHeader
        leading={
          <IconAction icon={ArrowLeft} label={t("back")} href={basePath} />
        }
        title={draft.title}
        description={t("subtitle", {
          code: problemId,
          topic: problemTopicLabel(topicList, draft.topic),
        })}
        actions={
          <>
            <Button
              variant="ghost"
              size="sm"
              className="border border-[var(--color-border)]"
            >
              {ta("previewAsLearner")}
            </Button>
            <Button asChild variant="cta" size="sm">
              <Link href={`${basePath}/${problemId}/edit`}>{t("edit")}</Link>
            </Button>
          </>
        }
      />

      <div className="grid grid-cols-1 items-start gap-4 xl:grid-cols-[minmax(0,1fr)_300px]">
        <div className="flex min-w-0 flex-col gap-3.5">
          <Card title={ta("statementTitle")}>
            <MarkdownPreview size="lg">{draft.body}</MarkdownPreview>
            <p className="mt-4 mb-1.5 text-[11px] font-semibold tracking-[0.07em] text-[var(--color-text-subtle)] uppercase">
              {ta("constraintsLabel")}
            </p>
            <pre className="font-mono text-[14px] whitespace-pre-wrap">
              {draft.constraints}
            </pre>
          </Card>

          <Card title={ta("examplesTitle")}>
            <div className="flex flex-col gap-2.5">
              {draft.examples.map((example, index) => (
                <div
                  key={example.id}
                  className="glass-surface rounded-2xl border border-[var(--color-border)] px-3.5 py-3"
                >
                  <p className="mb-2 text-[11px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
                    {ta("exampleIndex", { index: index + 1 })}
                  </p>
                  <dl className="flex flex-col gap-1.5 text-[14px]">
                    <div className="flex gap-2">
                      <dt className="w-20 shrink-0 text-[var(--color-text-subtle)]">
                        {ta("exampleInput")}
                      </dt>
                      <dd className="min-w-0 font-mono">{example.input}</dd>
                    </div>
                    <div className="flex gap-2">
                      <dt className="w-20 shrink-0 text-[var(--color-text-subtle)]">
                        {ta("exampleOutput")}
                      </dt>
                      <dd className="min-w-0 font-mono">{example.output}</dd>
                    </div>
                    <div className="flex gap-2">
                      <dt className="w-20 shrink-0 text-[var(--color-text-subtle)]">
                        {ta("exampleExplanation")}
                      </dt>
                      <dd className="min-w-0 text-[var(--color-text-muted)]">
                        {example.explanation}
                      </dd>
                    </div>
                  </dl>
                </div>
              ))}
            </div>
          </Card>

          <Card title={t("specTitle")} description={t("specSubtitle")}>
            {exceedsSchema(draft.spec) ? (
              <p className="mb-3 text-[12.5px] text-[var(--color-text-muted)]">
                {ta("spec.wrapperUnsupportedBody")}
              </p>
            ) : (
              <ul className="mb-4 flex flex-col gap-2">
                {SPEC_LANGUAGES.map((language) => (
                  <li
                    key={language}
                    className="flex flex-wrap items-baseline gap-x-3 gap-y-0.5"
                  >
                    <span className="w-16 shrink-0 text-[12px] font-semibold text-[var(--color-text-subtle)]">
                      {ta(`spec.language.${language}`)}
                    </span>
                    <code className="min-w-0 overflow-x-auto font-mono text-[12.5px] whitespace-pre">
                      {renderSignature(draft.spec.signature, language)}
                    </code>
                  </li>
                ))}
              </ul>
            )}
            <dl className="flex flex-col gap-3 text-[12.5px]">
              <div>
                <dt className="mb-1 text-[11px] font-semibold tracking-[0.07em] text-[var(--color-text-subtle)] uppercase">
                  {ta("spec.stdinFormat")}
                </dt>
                <dd className="whitespace-pre-wrap">
                  {draft.spec.stdinFormat}
                </dd>
              </div>
              <div>
                <dt className="mb-1 text-[11px] font-semibold tracking-[0.07em] text-[var(--color-text-subtle)] uppercase">
                  {ta("spec.stdoutFormat")}
                </dt>
                <dd className="whitespace-pre-wrap">
                  {draft.spec.stdoutFormat}
                </dd>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <dt className="text-[11px] font-semibold tracking-[0.07em] text-[var(--color-text-subtle)] uppercase">
                  {ta("spec.matchingStrategy")}
                </dt>
                <dd className="flex items-center gap-2">
                  <Badge variant="neutral">{draft.spec.matchingStrategy}</Badge>
                  {draft.spec.matchingStrategy === "EPSILON" ? (
                    <span className="font-mono">{draft.spec.epsilon}</span>
                  ) : null}
                </dd>
              </div>
            </dl>
          </Card>

          <Card
            title={ta("coverageTitle")}
            description={ta("coverageSubtitle")}
          >
            <div className="flex flex-wrap gap-2">
              {coverage.map((row) => (
                <Badge
                  key={row.category}
                  variant={row.approved > 0 ? "success" : "neutral"}
                >
                  {ta(`category.${row.category}`)}: {row.approved}
                </Badge>
              ))}
            </div>
          </Card>

          <Card
            title={ta("solutionTitle")}
            description={t("solutionSubtitle", {
              language: draft.solutionLanguage,
            })}
          >
            <details>
              <summary className="cursor-pointer text-[13px] font-semibold">
                {t("solutionToggle")}
              </summary>
              <pre className="mt-3 overflow-x-auto font-mono text-[12.5px] leading-relaxed">
                {draft.solution}
              </pre>
            </details>
          </Card>
        </div>

        <div className="flex flex-col gap-3.5 xl:sticky xl:top-[92px]">
          <Card title={ta("propertiesTitle")}>
            <dl className="flex flex-col gap-3 text-[13px]">
              <div className="flex items-center justify-between gap-2">
                <dt className="text-[var(--color-text-subtle)]">
                  {ta("difficultyLabel")}
                </dt>
                <dd>
                  <Badge
                    variant={problemLevelTone(levelList, draft.difficulty)}
                  >
                    {problemLevelLabel(levelList, draft.difficulty)}
                  </Badge>
                </dd>
              </div>
              <div className="flex items-center justify-between gap-2">
                <dt className="text-[var(--color-text-subtle)]">
                  {ta("statusLabel")}
                </dt>
                <dd>
                  <Badge
                    variant={
                      draft.status === "published" ? "success" : "neutral"
                    }
                  >
                    {ta(`status.${draft.status}`)}
                  </Badge>
                </dd>
              </div>
              <div className="flex items-center justify-between gap-2">
                <dt className="text-[var(--color-text-subtle)]">
                  {t("statTestcases")}
                </dt>
                <dd>
                  {approved.length}{" "}
                  <span className="text-[12px] text-[var(--color-text-muted)]">
                    {t("statTestcasesMeta", { count: pending })}
                  </span>
                </dd>
              </div>
              <div className="flex items-center justify-between gap-2">
                <dt className="text-[var(--color-text-subtle)]">
                  {t("statPublic")}
                </dt>
                <dd>{publicCount}</dd>
              </div>
              <div className="flex items-center justify-between gap-2">
                <dt className="text-[var(--color-text-subtle)]">
                  {t("statSolution")}
                </dt>
                <dd>
                  {draft.solutionCheck.ran
                    ? `${draft.solutionCheck.passed}/${draft.solutionCheck.total}`
                    : "-"}{" "}
                  <span className="text-[12px] text-[var(--color-text-muted)]">
                    {draft.solutionLanguage}
                  </span>
                </dd>
              </div>
              {LIMIT_KEYS.map((key) => (
                <div
                  key={key}
                  className="flex items-center justify-between gap-2"
                >
                  <dt className="text-[var(--color-text-subtle)]">
                    {ta(`limit.${key}`)}
                  </dt>
                  <dd className="font-mono">{draft.limits[key]}</dd>
                </div>
              ))}
            </dl>
          </Card>

          <Card title={ta("aiTitle")}>
            <ul className="mb-3 flex flex-col gap-2 text-[12.5px]">
              {AI_GUARD_KEYS.map((key: AiGuardKey) => (
                <li
                  key={key}
                  className="flex items-center justify-between gap-2"
                >
                  <span>{ta(`aiGuard.${key}.label`)}</span>
                  <Badge variant={draft.aiGuards[key] ? "success" : "neutral"}>
                    {draft.aiGuards[key] ? t("guardOn") : t("guardOff")}
                  </Badge>
                </li>
              ))}
            </ul>
            <p className="text-[12.5px] text-pretty text-[var(--color-text-muted)]">
              {draft.aiBrief || t("aiBriefEmpty")}
            </p>
          </Card>
        </div>
      </div>
    </div>
  );
}
