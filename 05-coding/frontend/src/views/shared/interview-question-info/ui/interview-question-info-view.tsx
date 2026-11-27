// Read-only detail page for one interview question (F6 #20, 03-dd/api/F6_interview-bank.md), split out of
// the authoring form: `/admin/interview-questions/[id]` shows this, `.../[id]/edit` shows
// InterviewQuestionAuthoringView. The route param is the question UUID (the code of the mock bank in mock
// mode); `IQ-%03d` is a label from `displayNo`. No mockup exists in 09-layoutBase for the authoring
// screen either, so this follows the same two-column structure: content on the wide side,
// classification and usage on the narrow side.
"use client";

import Link from "next/link";
import { useRef, useState } from "react";
import { ArrowLeft, Eye } from "lucide-react";
import {
  InterviewQuestionPreview,
  useAdminQuestion,
  levelTone,
  useInterviewLevels,
  useInterviewTopics,
  type AdminQuestionDetail,
} from "@/entities/interview-question";
import { useT } from "@/shared/i18n";
import {
  Badge,
  Button,
  Card,
  EmptyState,
  IconAction,
  MarkdownPreview,
  NoticeTile,
  PageContainer,
  PageHeader,
  ProgressBar,
  QueryBoundary,
  SegmentedTabs,
} from "@/shared/ui";

type Section = "question" | "followUps" | "rubric";

type Props = {
  /** Route param: a question code such as "IQ-014". */
  questionId: string;
  /** Area root this screen is mounted under, e.g. "/admin/interview-questions". */
  basePath: string;
};

export function InterviewQuestionInfoView({ questionId, basePath }: Props) {
  return (
    <QueryBoundary query={useAdminQuestion(questionId)}>
      {(question) => (
        <InterviewQuestionInfo
          questionId={questionId}
          basePath={basePath}
          question={question}
        />
      )}
    </QueryBoundary>
  );
}

function InterviewQuestionInfo({
  questionId,
  basePath,
  question,
}: Props & { question: AdminQuestionDetail | null }) {
  const t = useT("interviewQuestionInfo");
  const ta = useT("interviewQuestionAuthoring");
  const topicList = useInterviewTopics();
  const levelList = useInterviewLevels();
  const [section, setSection] = useState<Section>("question");
  const [previewOpen, setPreviewOpen] = useState(false);
  const sectionStart = useRef<HTMLDivElement>(null);

  if (!question) {
    return (
      <div>
        <PageHeader
          leading={
            <IconAction icon={ArrowLeft} label={t("back")} href={basePath} />
          }
          title={ta("notFoundTitle")}
        />
        <Card>
          <EmptyState>{ta("notFoundBody", { code: questionId })}</EmptyState>
        </Card>
      </div>
    );
  }

  const rubricTotal = question.rubric.reduce(
    (sum, criterion) => sum + criterion.weightPercent,
    0,
  );
  // Topic and level arrive as ids; the lists carry the id (API) or the key (mock) of each row.
  const topic = topicList.find(
    (item) => (item.id ?? item.key) === question.topicId,
  );
  const level = levelList.find(
    (item) => (item.id ?? item.key) === question.levelId,
  );

  return (
    <PageContainer variant="content" className="@container">
      <PageHeader
        className="static [&>div:nth-child(2)]:min-w-0 @max-[640px]:[&>div:last-child]:w-full @max-[640px]:[&>div:last-child]:justify-start"
        leading={
          <IconAction icon={ArrowLeft} label={t("back")} href={basePath} />
        }
        title={t("title", {
          code: `IQ-${String(question.displayNo).padStart(3, "0")}`,
        })}
        description={topic?.label}
        actions={
          <>
            <Button
              variant="ghost"
              size="sm"
              className="gap-1.5 border border-[var(--color-border)]"
              onClick={() => setPreviewOpen(true)}
            >
              <Eye aria-hidden="true" className="size-4" />
              {ta("previewAsLearner")}
            </Button>
            <Button asChild variant="cta" size="sm">
              <Link href={`${basePath}/${question.id}/edit`}>{t("edit")}</Link>
            </Button>
          </>
        }
      />

      <div ref={sectionStart} className="flex flex-1 flex-col gap-3">
        <div className="sticky top-0 z-20 w-[480px] max-w-full rounded-xl bg-[var(--color-background)]">
          <SegmentedTabs
            label={t("sectionsLabel")}
            value={section}
            onValueChange={(next) => {
              setSection(next);
              const start = sectionStart.current;
              const viewport = start?.closest("[data-page-viewport]");
              if (
                start &&
                start.getBoundingClientRect().top <
                  Math.max(0, viewport?.getBoundingClientRect().top ?? 0)
              ) {
                start.scrollIntoView({ block: "start" });
              }
            }}
            options={[
              { value: "question", label: ta("group1Title") },
              {
                value: "followUps",
                label: t("followUpsTab", { count: question.followUps.length }),
              },
              {
                value: "rubric",
                label: t("rubricTab", { count: question.rubric.length }),
              },
            ]}
            className="w-full flex-wrap [&>button]:grow"
          />
        </div>
        <div className="grid flex-1 items-start gap-4 @min-[900px]:grid-cols-[minmax(0,1fr)_clamp(16rem,25cqw,24rem)]">
          <div className="flex min-w-0 flex-col gap-4">
            {section === "question" && (
              <Card title={ta("group1Title")}>
                <MarkdownPreview
                  size="lg"
                  className="font-semibold wrap-anywhere"
                >
                  {question.title}
                </MarkdownPreview>
                {question.contentMarkdown ? (
                  <div className="mt-3">
                    <MarkdownPreview>
                      {question.contentMarkdown}
                    </MarkdownPreview>
                  </div>
                ) : null}
              </Card>
            )}

            {section === "followUps" && (
              <Card title={ta("group2Title")}>
                {question.followUps.length === 0 ? (
                  <p className="text-[14px] text-[var(--color-text-muted)]">
                    {t("followUpsEmpty")}
                  </p>
                ) : (
                  <ol className="list-decimal space-y-2 pl-5 text-sm leading-relaxed marker:font-semibold">
                    {question.followUps.map((followUp, index) => (
                      <li
                        key={index}
                        className="rounded-lg bg-[var(--color-track)] px-3 py-2.5 wrap-anywhere"
                      >
                        {followUp}
                      </li>
                    ))}
                  </ol>
                )}
              </Card>
            )}

            {section === "rubric" && (
              <Card
                title={ta("group3Title")}
                action={
                  question.rubric.length > 0 ? (
                    <span className="font-mono text-[12.5px] text-[var(--color-text-muted)]">
                      {ta("rubricTotal", { total: rubricTotal })}
                    </span>
                  ) : undefined
                }
              >
                {question.rubric.length === 0 ? (
                  <NoticeTile tone="info" title={ta("rubricEmptyTitle")}>
                    {ta("rubricEmptyBody")}
                  </NoticeTile>
                ) : (
                  <ul className="flex flex-col gap-2">
                    {question.rubric.map((criterion, index) => (
                      <li
                        key={index}
                        className="glass-surface space-y-2 rounded-xl border border-[var(--color-border)] px-3.5 py-2.5 text-sm"
                      >
                        <div className="flex items-start justify-between gap-3">
                          <span className="font-medium wrap-anywhere">
                            {criterion.criterionCode}
                          </span>
                          <span className="font-mono text-[var(--color-text-muted)]">
                            {criterion.weightPercent}%
                          </span>
                        </div>
                        <ProgressBar
                          value={criterion.weightPercent}
                          height={5}
                          label={criterion.criterionCode}
                        />
                        {criterion.description && (
                          <p className="text-sm whitespace-pre-wrap wrap-anywhere text-[var(--color-text-muted)]">
                            {criterion.description}
                          </p>
                        )}
                      </li>
                    ))}
                  </ul>
                )}
              </Card>
            )}
          </div>

          <div className="flex min-w-0 flex-col gap-4 @min-[900px]:sticky @min-[900px]:top-0">
            <Card title={ta("classifyTitle")}>
              <dl className="flex flex-col gap-3 text-[13px]">
                <div className="flex items-center justify-between gap-2">
                  <dt className="text-[var(--color-text-subtle)]">
                    {ta("topicLabel")}
                  </dt>
                  <dd>{topic?.label ?? "-"}</dd>
                </div>
                <div className="flex items-center justify-between gap-2">
                  <dt className="text-[var(--color-text-subtle)]">
                    {ta("levelLabel")}
                  </dt>
                  <dd>
                    <Badge
                      variant={
                        level ? levelTone(levelList, level.key) : "neutral"
                      }
                    >
                      {level?.label ?? "-"}
                    </Badge>
                  </dd>
                </div>
              </dl>
            </Card>

            <Card title={t("usageTitle")}>
              <dl className="flex flex-col gap-3 text-[13px]">
                <div className="flex items-center justify-between gap-2">
                  <dt className="text-[var(--color-text-subtle)]">
                    {t("usageCount")}
                  </dt>
                  <dd className="font-mono">{question.usageCount}</dd>
                </div>
                <div className="flex items-center justify-between gap-2">
                  <dt className="text-[var(--color-text-subtle)]">
                    {t("averageScore")}
                  </dt>
                  <dd className="font-mono">
                    {question.averageScorePercent === null
                      ? "-"
                      : `${Math.round(question.averageScorePercent)}%`}
                  </dd>
                </div>
                <div className="flex items-center justify-between gap-2">
                  <dt className="text-[var(--color-text-subtle)]">
                    {t("practiceMode")}
                  </dt>
                  <dd>
                    <Badge
                      variant={
                        question.practiceAvailable ? "success" : "neutral"
                      }
                    >
                      {question.practiceAvailable
                        ? t("practiceOn")
                        : t("practiceOff")}
                    </Badge>
                  </dd>
                </div>
              </dl>
            </Card>
          </div>
        </div>
      </div>
      {previewOpen && (
        <InterviewQuestionPreview
          title={question.title}
          content={question.contentMarkdown}
          followUps={question.followUps}
          onClose={() => setPreviewOpen(false)}
        />
      )}
    </PageContainer>
  );
}
