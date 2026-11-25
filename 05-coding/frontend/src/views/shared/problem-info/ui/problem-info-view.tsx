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
import { useRef, useState } from "react";
import { ArrowLeft } from "lucide-react";
import {
  AI_GUARD_KEYS,
  SPEC_LANGUAGES,
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
  type SpecLanguage,
  canViewerSeeProblem,
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
  PageContainer,
  PageHeader,
  SegmentedTabs,
  Skeleton,
} from "@/shared/ui";

import { TestcaseCoverage } from "./testcase-coverage";

const LIMIT_KEYS: (keyof ProblemLimits)[] = [
  "timeLimitMs",
  "memoryLimitMb",
  "outputLimitKb",
  "maxSubmissionsPerHour",
];

type DetailSection =
  "statement" | "examples" | "spec" | "coverage" | "solution";

type Props = {
  /** Problem identifier from the route. */
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
  const [section, setSection] = useState<DetailSection>("statement");
  const [signatureLanguage, setSignatureLanguage] =
    useState<SpecLanguage>("python");
  const sectionStart = useRef<HTMLDivElement>(null);

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
  const code = query.data.code;
  const topic = problemTopicLabel(topicList, draft.topic);

  // Real mode: a caller who may not read testcase bodies gets counts only (DD F2 #33), so the numbers
  // come from the summary and the coverage block, which needs each row's category, is left out.
  const server = query.data.server;
  const summaryOnly =
    server !== undefined &&
    draft.testcases.length === 0 &&
    server.testcaseSummary.total > 0;
  const approved = draft.testcases.filter((row) => row.approved);
  const approvedCount = summaryOnly
    ? server.testcaseSummary.approved
    : approved.length;
  const pending = summaryOnly
    ? server.testcaseSummary.total - server.testcaseSummary.approved
    : draft.testcases.length - approved.length;
  const publicCount = summaryOnly
    ? server.testcaseSummary.sample
    : approved.filter((row) => row.visibility === "public").length;
  const sections: { value: DetailSection; label: string }[] = [
    { value: "statement", label: ta("statementTitle") },
    {
      value: "examples",
      label: `${ta("examplesTitle")} (${draft.examples.length})`,
    },
    { value: "spec", label: t("sectionSpec") },
  ];
  if (!summaryOnly)
    sections.push({ value: "coverage", label: t("sectionCoverage") });
  if (!server?.readOnly)
    sections.push({ value: "solution", label: ta("solutionTitle") });
  const activeSection = sections.some(({ value }) => value === section)
    ? section
    : "statement";
  const solutionLanguage = SPEC_LANGUAGES.find(
    (language) => language === draft.solutionLanguage.toLowerCase(),
  );

  return (
    <PageContainer variant="content" className="@container">
      <PageHeader
        className="static"
        leading={
          <IconAction icon={ArrowLeft} label={t("back")} href={basePath} />
        }
        title={draft.title}
        description={
          <span className="block whitespace-normal [overflow-wrap:anywhere]">
            {code
              ? t("subtitle", { code, topic })
              : t("subtitleTopic", { topic })}
          </span>
        }
        actions={
          <>
            {/* Same target as the authoring screen's eye icon: SHR0202 Q6 (owner 2026-10-03) opens
                the last saved data in a new tab. The button did nothing before (UX review B9). */}
            <Button
              asChild
              variant="ghost"
              size="sm"
              className="border border-[var(--color-border)]"
            >
              <a
                href={`${basePath}/${problemId}/preview`}
                target="_blank"
                rel="noopener noreferrer"
              >
                {ta("previewAsLearner")}
              </a>
            </Button>
            {/* A2 reads other authors' published problems but never edits them. */}
            {(server ? !server.readOnly : canViewerSeeProblem(problemId)) ? (
              <Button asChild variant="cta" size="sm">
                <Link href={`${basePath}/${problemId}/edit`}>{t("edit")}</Link>
              </Button>
            ) : null}
          </>
        }
      />

      <div ref={sectionStart} className="flex flex-1 flex-col gap-3">
        <div className="sticky top-0 z-20 w-[480px] max-w-full rounded-xl bg-[var(--color-background)]">
          <SegmentedTabs
            label={t("sectionsLabel")}
            options={sections}
            value={activeSection}
            onValueChange={(nextSection) => {
              setSection(nextSection);
              const start = sectionStart.current;
              const viewport = start?.closest("[data-page-viewport]");
              const top = Math.max(
                0,
                viewport?.getBoundingClientRect().top ?? 0,
              );
              if (start && start.getBoundingClientRect().top < top) {
                start.scrollIntoView({ block: "start" });
              }
            }}
            className="w-full flex-wrap [&>button]:grow"
          />
        </div>

        <div className="grid flex-1 grid-cols-1 gap-4 @min-[960px]:grid-cols-[minmax(0,4fr)_minmax(16rem,1fr)]">
          <div className="flex min-w-0 flex-col @min-[960px]:[&>section]:flex-1">
            {activeSection === "statement" && (
              <Card title={ta("statementTitle")}>
                <MarkdownPreview size="lg">{draft.body}</MarkdownPreview>
                {draft.constraints.trim() && (
                  <div className="mt-4 border-t border-[var(--color-border)] pt-3">
                    <h3 className="mb-1 text-sm font-semibold">
                      {ta("constraintsLabel")}
                    </h3>
                    <pre className="font-mono text-sm whitespace-pre-wrap wrap-anywhere">
                      {draft.constraints}
                    </pre>
                  </div>
                )}
              </Card>
            )}

            {activeSection === "examples" && (
              <Card title={ta("examplesTitle")}>
                {draft.examples.length === 0 ? (
                  <p className="text-sm text-[var(--color-text-muted)]">
                    {ta("emptyExamples")}
                  </p>
                ) : (
                  <div className="grid grid-cols-[repeat(auto-fit,minmax(min(100%,22rem),1fr))] gap-3">
                    {draft.examples.map((example, index) => (
                      <article
                        key={example.id}
                        className="min-w-0 rounded-xl border border-[var(--color-border)] p-3"
                      >
                        <h3 className="mb-3 text-sm font-semibold">
                          {ta("exampleIndex", { index: index + 1 })}
                        </h3>
                        <dl className="grid gap-2 sm:grid-cols-2">
                          <div className="min-w-0 rounded-lg bg-[var(--color-track)] p-2.5">
                            <dt className="mb-1 text-xs font-semibold text-[var(--color-text-muted)]">
                              {ta("exampleInput")}
                            </dt>
                            <dd>
                              <pre className="font-mono text-sm whitespace-pre-wrap wrap-anywhere">
                                {example.input}
                              </pre>
                            </dd>
                          </div>
                          <div className="min-w-0 rounded-lg bg-[var(--color-track)] p-2.5">
                            <dt className="mb-1 text-xs font-semibold text-[var(--color-text-muted)]">
                              {ta("exampleOutput")}
                            </dt>
                            <dd>
                              <pre className="font-mono text-sm whitespace-pre-wrap wrap-anywhere">
                                {example.output}
                              </pre>
                            </dd>
                          </div>
                          {example.explanation.trim() && (
                            <div className="min-w-0 text-sm sm:col-span-2">
                              <dt className="font-semibold">
                                {ta("exampleExplanation")}
                              </dt>
                              <dd className="mt-1 wrap-anywhere text-[var(--color-text-muted)]">
                                {example.explanation}
                              </dd>
                            </div>
                          )}
                        </dl>
                      </article>
                    ))}
                  </div>
                )}
              </Card>
            )}

            {activeSection === "spec" && (
              <Card title={t("specTitle")}>
                <dl className="grid gap-3 text-sm sm:grid-cols-2">
                  <div className="min-w-0 rounded-lg bg-[var(--color-track)] p-3">
                    <dt className="mb-1 font-semibold">
                      {ta("spec.stdinFormat")}
                    </dt>
                    <dd className="whitespace-pre-wrap wrap-anywhere">
                      {draft.spec.stdinFormat || t("unspecified")}
                    </dd>
                  </div>
                  <div className="min-w-0 rounded-lg bg-[var(--color-track)] p-3">
                    <dt className="mb-1 font-semibold">
                      {ta("spec.stdoutFormat")}
                    </dt>
                    <dd className="whitespace-pre-wrap wrap-anywhere">
                      {draft.spec.stdoutFormat || t("unspecified")}
                    </dd>
                  </div>
                  <div className="min-w-0 rounded-lg border border-[var(--color-border)] p-3 sm:col-span-2">
                    <dt className="flex flex-wrap items-center gap-2 font-semibold">
                      {ta("spec.matchingStrategy")}
                      <Badge variant="neutral">
                        {draft.spec.matchingStrategy}
                      </Badge>
                    </dt>
                    <dd className="mt-1 text-[var(--color-text-muted)]">
                      {ta(`spec.strategy.${draft.spec.matchingStrategy}.meta`)}
                      {draft.spec.matchingStrategy === "EPSILON" && (
                        <span className="ml-2 font-mono">
                          ε = {draft.spec.epsilon}
                        </span>
                      )}
                    </dd>
                  </div>
                </dl>
                <div className="mt-4 border-t border-[var(--color-border)] pt-3">
                  <h3 className="mb-2 text-sm font-semibold">
                    {ta("spec.signatureTitle")}
                  </h3>
                  {exceedsSchema(draft.spec) ? (
                    <p className="text-sm text-[var(--color-text-muted)]">
                      {ta("spec.wrapperUnsupportedBody")}
                    </p>
                  ) : !draft.spec.signature.functionName.trim() ? (
                    <p className="text-sm text-[var(--color-text-muted)]">
                      {t("unspecified")}
                    </p>
                  ) : (
                    <>
                      <SegmentedTabs
                        label={ta("spec.preview.languages")}
                        options={SPEC_LANGUAGES.map((language) => ({
                          value: language,
                          label: ta(`spec.language.${language}`),
                        }))}
                        value={signatureLanguage}
                        onValueChange={setSignatureLanguage}
                        className="w-fit"
                      />
                      <pre className="mt-2 overflow-x-auto rounded-lg bg-[var(--color-track)] p-3 text-sm">
                        <code>
                          {renderSignature(
                            draft.spec.signature,
                            signatureLanguage,
                          )}
                        </code>
                      </pre>
                    </>
                  )}
                </div>
              </Card>
            )}

            {activeSection === "coverage" && !summaryOnly && (
              <TestcaseCoverage
                rows={draft.testcases}
                approvedCount={approvedCount}
                problemId={server && !server.readOnly ? problemId : undefined}
              />
            )}

            {activeSection === "solution" && !server?.readOnly && (
              <Card
                title={ta("solutionTitle")}
                action={
                  <Badge variant="neutral">
                    {solutionLanguage
                      ? ta(`spec.language.${solutionLanguage}`)
                      : draft.solutionLanguage}
                  </Badge>
                }
              >
                {draft.solution.trim() ? (
                  <pre className="overflow-x-auto rounded-lg bg-[var(--color-track)] p-3 text-sm leading-relaxed">
                    <code>{draft.solution}</code>
                  </pre>
                ) : (
                  <p className="text-sm text-[var(--color-text-muted)]">
                    {t("solutionEmpty")}
                  </p>
                )}
              </Card>
            )}
          </div>

          <div className="flex min-w-0 flex-col gap-3.5 self-start @min-[960px]:sticky @min-[960px]:top-0">
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
                    {approvedCount}{" "}
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
                    <dd className="font-mono">
                      {draft.limits[key] ?? ta("limit.systemDefault")}
                    </dd>
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
                    <Badge
                      variant={draft.aiGuards[key] ? "success" : "neutral"}
                    >
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
    </PageContainer>
  );
}
