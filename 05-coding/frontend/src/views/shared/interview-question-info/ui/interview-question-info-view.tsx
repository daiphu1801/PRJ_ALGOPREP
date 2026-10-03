// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Read-only detail page for one interview question, split out of the authoring form:
// `/admin/interview-questions/[code]` shows this, `.../[code]/edit` shows
// InterviewQuestionAuthoringView unchanged. No mockup exists in 09-layoutBase for the authoring
// screen either, so this follows the same two-column structure: content on the wide side,
// classification and usage on the narrow side.
"use client";

import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import {
  findInterviewQuestionByCode,
  topicLabel,
  useInterviewTopics,
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
  PageHeader,
} from "@/shared/ui";

const LEVEL_VARIANT = { easy: "success", medium: "warn", hard: "negative" } as const;

type Props = {
  /** Route param: a question code such as "IQ-014". */
  questionId: string;
  /** Area root this screen is mounted under, e.g. "/admin/interview-questions". */
  basePath: string;
};

export function InterviewQuestionInfoView({ questionId, basePath }: Props) {
  const t = useT("interviewQuestionInfo");
  const ta = useT("interviewQuestionAuthoring");
  const topicList = useInterviewTopics();
  const question = findInterviewQuestionByCode(questionId);

  if (!question) {
    return (
      <div>
        <PageHeader
          leading={<IconAction icon={ArrowLeft} label={t("back")} href={basePath} />}
          title={ta("notFoundTitle")}
        />
        <Card>
          <EmptyState>{ta("notFoundBody", { code: questionId })}</EmptyState>
        </Card>
      </div>
    );
  }

  const rubricTotal = question.rubric.reduce((sum, criterion) => sum + criterion.weight, 0);

  return (
    <div>
      <PageHeader
        leading={<IconAction icon={ArrowLeft} label={t("back")} href={basePath} />}
        title={t("title", { code: question.code })}
        description={topicLabel(topicList, question.topic)}
        actions={
          <>
            <Button variant="ghost" size="sm" className="border border-[var(--color-border)]">
              {ta("previewAsLearner")}
            </Button>
            <Button asChild variant="cta" size="sm">
              <Link href={`${basePath}/${question.code}/edit`}>{t("edit")}</Link>
            </Button>
          </>
        }
      />

      <div className="grid items-start gap-4 lg:grid-cols-[minmax(0,1fr)_320px]">
        <div className="flex min-w-0 flex-col gap-4">
          <Card title={ta("group1Title")}>
            <MarkdownPreview>{question.question}</MarkdownPreview>
          </Card>

          <Card title={ta("group2Title")}>
            {question.followUps.length === 0 ? (
              <p className="text-[13px] text-[var(--color-text-muted)]">{t("followUpsEmpty")}</p>
            ) : (
              <ol className="flex list-decimal flex-col gap-2 pl-5 text-[13px]">
                {question.followUps.map((followUp) => (
                  <li key={followUp}>{followUp}</li>
                ))}
              </ol>
            )}
          </Card>

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
                {question.rubric.map((criterion) => (
                  <li
                    key={criterion.label}
                    className="glass-surface flex items-center justify-between gap-3 rounded-2xl border border-[var(--color-border)] px-3.5 py-2.5 text-[13px]"
                  >
                    <span>{criterion.label}</span>
                    <span className="font-mono text-[var(--color-text-muted)]">{criterion.weight}%</span>
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </div>

        <div className="flex min-w-0 flex-col gap-4 lg:sticky lg:top-[90px]">
          <Card title={ta("classifyTitle")}>
            <dl className="flex flex-col gap-3 text-[13px]">
              <div className="flex items-center justify-between gap-2">
                <dt className="text-[var(--color-text-subtle)]">{ta("topicLabel")}</dt>
                <dd>{topicLabel(topicList, question.topic)}</dd>
              </div>
              <div className="flex items-center justify-between gap-2">
                <dt className="text-[var(--color-text-subtle)]">{ta("levelLabel")}</dt>
                <dd>
                  <Badge variant={LEVEL_VARIANT[question.level]}>{ta(`level.${question.level}`)}</Badge>
                </dd>
              </div>
            </dl>
          </Card>

          <Card title={t("usageTitle")}>
            <dl className="flex flex-col gap-3 text-[13px]">
              <div className="flex items-center justify-between gap-2">
                <dt className="text-[var(--color-text-subtle)]">{t("usageCount")}</dt>
                <dd className="font-mono">{question.usageCount}</dd>
              </div>
              <div className="flex items-center justify-between gap-2">
                <dt className="text-[var(--color-text-subtle)]">{t("averageScore")}</dt>
                <dd className="font-mono">{question.averageScore.toFixed(1)} / 5</dd>
              </div>
              <div className="flex items-center justify-between gap-2">
                <dt className="text-[var(--color-text-subtle)]">{t("practiceMode")}</dt>
                <dd>
                  <Badge variant={question.hasRubric ? "success" : "neutral"}>
                    {question.hasRubric ? t("practiceOn") : t("practiceOff")}
                  </Badge>
                </dd>
              </div>
            </dl>
          </Card>
        </div>
      </div>
    </div>
  );
}
