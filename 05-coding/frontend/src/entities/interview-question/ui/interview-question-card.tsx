// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// One question card: code + level + topic header, the question itself, the follow-up probes, the
// scoring rubric, then a usage line and the row of actions.
// 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:182-233.
//
// Lives in entities/, not shared/: it takes an `InterviewQuestion`, so by the layering test in
// 01-rd/system/frontend_architecture.md section 2.A it only has meaning inside this domain. The
// student-facing interview bank screen will reuse it with `actions` left out.
"use client";

import type { ReactNode } from "react";
import { Badge, Card, ProgressBar, type BadgeVariant } from "@/shared/ui";
import type { InterviewQuestion, QuestionLevel } from "../model/types";

// dc.html:437-441.
const LEVEL_VARIANT: Record<QuestionLevel, BadgeVariant> = {
  easy: "success",
  medium: "blue",
  hard: "purple",
};

const RUBRIC_COLOR_VARS = [
  "--color-admin-teal",
  "--color-admin-warn",
  "--color-success",
  "--color-admin-cyan",
];

export function InterviewQuestionCard({
  question,
  topicLabel,
  levelLabel,
  followUpsLabel,
  rubricLabel,
  usageLabel,
  actions,
}: {
  question: InterviewQuestion;
  topicLabel: string;
  levelLabel: string;
  followUpsLabel: string;
  rubricLabel: string;
  /** Pre-formatted, e.g. "Dùng 184 lần · điểm TB 3.8/5". */
  usageLabel: string;
  actions?: ReactNode;
}) {
  const sectionHeading =
    "mb-2 text-[10.5px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase";

  return (
    <Card className="flex flex-col gap-3 px-[19px]" headingLevel={3}>
      <div className="flex flex-wrap items-center gap-2">
        <span className="font-mono text-[11.5px] text-[var(--color-text-subtle)]">
          {question.code}
        </span>
        <Badge variant={LEVEL_VARIANT[question.level]}>{levelLabel}</Badge>
        <span className="ml-auto text-[11.5px] whitespace-nowrap text-[var(--color-text-muted)]">
          {topicLabel}
        </span>
      </div>

      <h3 className="text-[14.5px] leading-snug font-semibold text-pretty">{question.question}</h3>

      <div className="glass-surface rounded-2xl border border-[var(--color-border)] px-3 py-3">
        <p className={sectionHeading}>{followUpsLabel}</p>
        <ul className="flex flex-col gap-1.5">
          {question.followUps.map((followUp) => (
            <li
              key={followUp}
              className="flex gap-2 text-[12.5px] leading-relaxed text-pretty text-[var(--color-text-muted)]"
            >
              {/* An arrow as a bullet, matching dc.html:460 — a plain character, not a pictograph. */}
              <span aria-hidden="true" className="shrink-0 text-[var(--color-text-subtle)]">
                →
              </span>
              <span>{followUp}</span>
            </li>
          ))}
        </ul>
      </div>

      <div>
        <p className={sectionHeading}>{rubricLabel}</p>
        <div className="flex flex-col gap-2">
          {question.rubric.map((criterion, index) => (
            <div key={criterion.label}>
              <div className="mb-1 flex items-center justify-between gap-2.5">
                <span className="text-[12.5px] font-medium">{criterion.label}</span>
                <span className="font-mono text-[11.5px] text-[var(--color-text-subtle)]">
                  {criterion.weight}%
                </span>
              </div>
              <ProgressBar
                value={criterion.weight}
                height={5}
                label={criterion.label}
                fill={`var(${RUBRIC_COLOR_VARS[index % RUBRIC_COLOR_VARS.length]})`}
              />
            </div>
          ))}
        </div>
      </div>

      <div className="flex items-center gap-2.5 border-t border-[var(--color-border)] pt-3">
        <span className="text-xs whitespace-nowrap text-[var(--color-text-subtle)]">
          {usageLabel}
        </span>
        {actions ? <span className="ml-auto flex gap-1.5">{actions}</span> : null}
      </div>
    </Card>
  );
}
