// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md.
//
// One row in the browse-mode list of USR0401 — topic, difficulty, recall-status badge, title, and
// a bookmark toggle. 09-layoutBase/Câu hỏi phỏng vấn.dc.html:132-141 (the admin card in this same
// entity is a different shape — that one is for SHR0301's card grid, this is the compact row the
// student list needs [SoT: 02-bd/screens/users/USR0401_interview_bank_list.md Sheet 4.4]).
"use client";

import { cn } from "@/shared/lib";
import { Badge, type BadgeVariant } from "@/shared/ui";
import { BookmarkToggle } from "./bookmark-toggle";
import type { InterviewQuestion, RecallLevel } from "../model/types";

const RECALL_VARIANT: Record<RecallLevel, BadgeVariant> = {
  known: "success",
  vague: "warn",
  forgotten: "warn",
};

export function InterviewQuestionListRow({
  question,
  topicLabel,
  levelLabel,
  levelTone,
  recallLabel,
  recall,
  selected,
  bookmarked,
  bookmarkLabel,
  onSelect,
  onToggleBookmark,
}: {
  question: InterviewQuestion;
  topicLabel: string;
  levelLabel: string;
  levelTone: BadgeVariant;
  recallLabel: string;
  recall: RecallLevel | null;
  selected: boolean;
  bookmarked: boolean;
  bookmarkLabel: string;
  onSelect: () => void;
  onToggleBookmark: () => void;
}) {
  return (
    <div
      className={cn(
        "flex items-start gap-2.5 border-b border-[var(--color-border)] px-4 py-3 last:border-b-0",
        selected
          ? "bg-[var(--color-surface-hover)]"
          : "hover:bg-[var(--color-surface-hover)]",
      )}
    >
      <button
        type="button"
        onClick={onSelect}
        aria-pressed={selected}
        className="min-w-0 flex-1 text-left"
      >
        <div className="mb-1.5 flex flex-wrap items-center gap-2">
          <span className="text-[10.5px] font-semibold tracking-[0.06em] text-[var(--color-text-subtle)] uppercase">
            {topicLabel}
          </span>
          <Badge variant={levelTone}>{levelLabel}</Badge>
          <span className="ml-auto">
            <Badge variant={recall ? RECALL_VARIANT[recall] : "neutral"}>
              {recallLabel}
            </Badge>
          </span>
        </div>
        <p className="text-[13.5px] leading-snug font-medium text-pretty">
          {question.question}
        </p>
      </button>
      <BookmarkToggle
        bookmarked={bookmarked}
        onToggle={onToggleBookmark}
        label={bookmarkLabel}
      />
    </div>
  );
}
