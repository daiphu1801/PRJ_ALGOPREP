// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md mục 9.1 (`views/interview-question-detail`).
//
// USR0402 `interview_question_detail` has no prototype OF ITS OWN, but it is not UI invented from
// nothing either: most of Chế độ học reuses the "xem nhanh" block already dựng for
// `interview_bank_list`, and Chế độ luyện's input pattern is modeled on `mock_interview`'s textarea.
// Layout follows 02-bd/screens/users/USR0402_interview_question_detail.md Sheet 4.4 (regions A-F);
// per-region source:
// - A. Đầu trang câu hỏi (topic/level badge, title, content, bookmark) — reused verbatim from the
//   quick-view block [SoT: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:150-155].
// - C. Chế độ học — "Ý cần nói" outline list [SoT: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:157-165];
//   recall-level buttons follow the same block's "Mức độ nắm" control [SoT:
//   09-layoutBase/Câu hỏi phỏng vấn.dc.html:181-185]. Answer framework and core-keywords blocks have
//   NO prototype backing — new per BD mục 4.4 [SoT: Suy luận].
// - D. Chế độ luyện — textarea + button row modeled on `mock_interview`'s answer input [SoT:
//   09-layoutBase/Phỏng vấn giả lập.dc.html:307-316]; the AI-unavailable notice follows that
//   screen's error state [SoT: 09-layoutBase/Phỏng vấn giả lập.dc.html:295-304].
// - B (mode toggle), E (attempt history), F (leave-confirm popup) have no prototype at all — pure
//   BD/RD inference [SoT: 02-bd/screens/users/USR0402_interview_question_detail.md mục 4.4].
//
// "Bẫy thường gặp" (dc.html:167-172) is deliberately NOT its own UI block here — BD Sheet 4 Câu hỏi
// mở Q1 folds it into `suggested_approach` instead of adding a DB column, so there is nothing to
// render separately [SoT: 02-bd/screens/users/USR0402_interview_question_detail.md:628].
//
// Content/framework text is rendered as plain text (`whitespace-pre-line`), not real Markdown —
// no Markdown renderer is installed in this codebase yet and adding one for two fields is not
// justified at prototype stage.
"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  findInterviewQuestionByCode,
  useRecallAndBookmarkState,
  RecallLevelPicker,
  type AnswerAttempt,
  type RecallLevel,
} from "@/entities/interview-question";
import { useT } from "@/shared/i18n";
import { Badge, Button, Card, ConfirmDialog, EmptyState, NoticeTile, TextArea } from "@/shared/ui";

// [SoT: Suy luận] — BD Sheet 9 NO 6 để ngỏ giới hạn độ dài cụ thể (Câu hỏi mở Q3); 4000 ký tự đủ
// cho một câu trả lời phỏng vấn dài mà vẫn chặn được việc dán nguyên một tài liệu.
const ANSWER_MAX_LENGTH = 4000;

type Mode = "study" | "practice";

export function InterviewQuestionDetailView({ questionId }: { questionId: string }) {
  const t = useT("interviewQuestionDetail");
  const router = useRouter();
  const [question] = useState(() => findInterviewQuestionByCode(questionId));

  // Hook cần một mảng — câu hỏi đơn lẻ vẫn seed đúng một khoá. Câu hỏi không tồn tại thì mảng rỗng,
  // hook vẫn an toàn (map rỗng).
  const { recall, bookmarks, rate, toggleBookmark } = useRecallAndBookmarkState(
    question ? [question] : [],
  );

  const [mode, setMode] = useState<Mode>("study");
  const [answerText, setAnswerText] = useState("");
  const [attempts, setAttempts] = useState<AnswerAttempt[]>(question?.attempts ?? []);
  const [pendingFeedback, setPendingFeedback] = useState(false);
  const [expandedAttempt, setExpandedAttempt] = useState<number | null>(null);
  const [leaveTarget, setLeaveTarget] = useState<string | null>(null);

  if (!question) {
    return (
      <div className="p-6">
        <Card>
          <EmptyState>{t("notFound")}</EmptyState>
          <div className="mt-3 text-center">
            <Link href="/interview-bank" className="text-[13px] font-semibold text-[var(--color-primary)] hover:underline">
              {t("header.linkBack")}
            </Link>
          </div>
        </Card>
      </div>
    );
  }

  const latestAttempt = attempts[0] ?? null;
  const hasDraft = mode === "practice" && answerText.trim().length > 0 && !latestAttempt;

  const recallLabels: Record<RecallLevel, string> = {
    known: t("study.recall.known"),
    vague: t("study.recall.vague"),
    forgotten: t("study.recall.forgotten"),
  };

  function guardedNavigate(href: string) {
    if (hasDraft) {
      setLeaveTarget(href);
      return;
    }
    router.push(href);
  }

  function submitAnswer() {
    const trimmed = answerText.trim();
    if (!trimmed || trimmed.length > ANSWER_MAX_LENGTH || pendingFeedback) return;
    setPendingFeedback(true);
    const attemptNo = attempts.length + 1;
    // Không gọi AI thật (prototype) — mô phỏng độ trễ và một phản hồi mẫu bám theo rubric của câu
    // hỏi, để khối phản hồi có nội dung liên quan thay vì text vô nghĩa cố định.
    // `question` is narrowed non-null by the early return above, but that narrowing does not
    // survive into a closure — `setTimeout`'s callback runs later, so TS re-widens it to
    // `| undefined` here. The value itself cannot actually change (seeded once via useState).
    const rubric = question!.rubric;
    window.setTimeout(() => {
      const strengths = rubric.slice(0, 1).map((c) => t("practice.feedbackSample.strength", { criterion: c.label }));
      const gaps = rubric.slice(1, 2).map((c) => t("practice.feedbackSample.gap", { criterion: c.label }));
      setAttempts((prev) => [
        {
          attemptNo,
          createdAt: new Date().toISOString(),
          feedbackStatus: "completed",
          answerText: trimmed,
          strengths,
          gaps,
          nextSteps: t("practice.feedbackSample.nextSteps"),
        },
        ...prev,
      ]);
      setPendingFeedback(false);
    }, 900);
  }

  function retryPractice() {
    setAnswerText("");
  }

  return (
    <div className="mx-auto max-w-[860px] p-6">
      <ConfirmDialog
        open={leaveTarget !== null}
        onClose={() => setLeaveTarget(null)}
        onConfirm={() => {
          const target = leaveTarget;
          setLeaveTarget(null);
          if (target) router.push(target);
        }}
        title={t("leaveConfirm.message")}
        confirmLabel={t("leaveConfirm.btnLeave")}
        cancelLabel={t("leaveConfirm.btnStay")}
        destructive
      >
        {t("leaveConfirm.message")}
      </ConfirmDialog>

      <div className="mb-4 flex items-center justify-between gap-3">
        <button
          type="button"
          onClick={() => guardedNavigate("/interview-bank")}
          className="text-[13px] font-semibold text-[var(--color-primary)] hover:underline"
        >
          {t("header.linkBack")}
        </button>
        <Button
          variant="ghost"
          size="sm"
          className="border border-[var(--color-border)]"
          aria-pressed={bookmarks[question.code] ?? false}
          onClick={() => toggleBookmark(question.code)}
        >
          {(bookmarks[question.code] ?? false) ? t("header.bookmarked") : t("header.bookmark")}
        </Button>
      </div>

      <Card className="mb-4">
        <div className="mb-2 flex items-center gap-2">
          <Badge variant="neutral">{t(`topic.${question.topic}`)}</Badge>
          <span className="text-[11.5px] font-semibold text-[var(--color-text-muted)]">
            {t(`level.${question.level}`)}
          </span>
        </div>
        <h1 className="mb-3 text-xl font-semibold text-pretty">{question.question}</h1>
        <p className="text-[13.5px] whitespace-pre-line text-[var(--color-text-muted)]">
          {question.content}
        </p>
      </Card>

      <div role="tablist" aria-label={t("mode.groupLabel")} className="mb-4 flex gap-2">
        <Button
          role="tab"
          aria-selected={mode === "study"}
          variant={mode === "study" ? "primary" : "ghost"}
          size="sm"
          className={mode !== "study" ? "border border-[var(--color-border)]" : undefined}
          onClick={() => setMode("study")}
        >
          {t("mode.study")}
        </Button>
        <Button
          role="tab"
          aria-selected={mode === "practice"}
          variant={mode === "practice" ? "primary" : "ghost"}
          size="sm"
          className={mode !== "practice" ? "border border-[var(--color-border)]" : undefined}
          disabled={!question.hasRubric}
          title={!question.hasRubric ? t("mode.practiceLockedReason") : undefined}
          onClick={() => setMode("practice")}
        >
          {t("mode.practice")}
        </Button>
      </div>
      {!question.hasRubric && mode === "study" ? (
        <NoticeTile tone="info" title={t("mode.practiceLockedReason")} className="mb-4" />
      ) : null}

      {mode === "study" ? (
        <Card className="mb-4 flex flex-col gap-5">
          <div>
            <p className="mb-2 text-[10.5px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
              {t("study.outlineLabel")}
            </p>
            <ul className="flex flex-col gap-2">
              {question.suggestedApproach.map((point, index) => (
                <li key={point} className="flex gap-2.5 text-[13.5px] text-[var(--color-text-muted)]">
                  <span className="font-mono text-[11.5px] font-semibold text-[var(--color-primary)]">
                    {String(index + 1).padStart(2, "0")}
                  </span>
                  <span>{point}</span>
                </li>
              ))}
            </ul>
          </div>

          <div>
            <p className="mb-2 text-[10.5px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
              {t("study.answerFrameworkLabel")}
            </p>
            <p className="text-[13.5px] whitespace-pre-line text-[var(--color-text)]">
              {question.sampleAnswerFramework}
            </p>
          </div>

          {question.coreKeywords.length > 0 ? (
            <div>
              <p className="mb-2 text-[10.5px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
                {t("study.keywordsLabel")}
              </p>
              <div className="flex flex-wrap gap-1.5">
                {question.coreKeywords.map((keyword) => (
                  <Badge key={keyword} variant="neutral">
                    {keyword}
                  </Badge>
                ))}
              </div>
            </div>
          ) : null}

          {question.followUps.length > 0 ? (
            <div>
              <p className="mb-2 text-[10.5px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
                {t("study.followUpsLabel")}
              </p>
              <ul className="flex flex-col gap-1.5">
                {question.followUps.map((followUp) => (
                  <li key={followUp} className="flex gap-2 text-[13px] text-[var(--color-text-muted)]">
                    <span aria-hidden="true" className="shrink-0 text-[var(--color-text-subtle)]">
                      →
                    </span>
                    <span>{followUp}</span>
                  </li>
                ))}
              </ul>
            </div>
          ) : null}

          <div className="border-t border-[var(--color-border)] pt-4">
            <p className="mb-2 text-[10.5px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
              {t("study.recallLabel")}
            </p>
            <RecallLevelPicker
              value={recall[question.code] ?? null}
              onRate={(level) => rate(question.code, level)}
              labels={recallLabels}
              groupLabel={t("study.recallLabel")}
            />
          </div>
        </Card>
      ) : (
        <Card className="mb-4 flex flex-col gap-3">
          <TextArea
            label={t("practice.answerLabel")}
            value={answerText}
            onChange={(event) => setAnswerText(event.target.value.slice(0, ANSWER_MAX_LENGTH))}
            disabled={pendingFeedback || latestAttempt !== null}
            rows={8}
            placeholder={t("practice.answerPlaceholder")}
          />
          <div className="flex items-center justify-between gap-3">
            <span className="text-[12px] text-[var(--color-text-subtle)]">
              {t("practice.charCounter", { count: answerText.length, max: ANSWER_MAX_LENGTH })}
            </span>
            {latestAttempt ? (
              <Button variant="ghost" size="sm" className="border border-[var(--color-border)]" onClick={retryPractice}>
                {t("practice.btnRetry")}
              </Button>
            ) : (
              <Button
                size="sm"
                disabled={!answerText.trim() || answerText.length > ANSWER_MAX_LENGTH || pendingFeedback}
                aria-busy={pendingFeedback || undefined}
                onClick={submitAnswer}
              >
                {pendingFeedback ? t("practice.pendingState") : t("practice.btnSubmit")}
              </Button>
            )}
          </div>

          {latestAttempt ? (
            <div className="mt-2 flex flex-col gap-3 border-t border-[var(--color-border)] pt-4">
              {latestAttempt.feedbackStatus === "failed" ? (
                <NoticeTile tone="warn" title={t("practice.aiUnavailable")} />
              ) : (
                <>
                  {latestAttempt.strengths?.length ? (
                    <div>
                      <p className="mb-1.5 text-[10.5px] font-semibold tracking-[0.08em] text-[var(--color-success-text)] uppercase">
                        {t("practice.strengthsLabel")}
                      </p>
                      <ul className="list-disc pl-5 text-[13px] text-[var(--color-text-muted)]">
                        {latestAttempt.strengths.map((item) => (
                          <li key={item}>{item}</li>
                        ))}
                      </ul>
                    </div>
                  ) : null}
                  {latestAttempt.gaps?.length ? (
                    <div>
                      <p className="mb-1.5 text-[10.5px] font-semibold tracking-[0.08em] text-[var(--color-admin-warn)] uppercase">
                        {t("practice.gapsLabel")}
                      </p>
                      <ul className="list-disc pl-5 text-[13px] text-[var(--color-text-muted)]">
                        {latestAttempt.gaps.map((item) => (
                          <li key={item}>{item}</li>
                        ))}
                      </ul>
                    </div>
                  ) : null}
                  {latestAttempt.nextSteps ? (
                    <div>
                      <p className="mb-1.5 text-[10.5px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
                        {t("practice.nextStepsLabel")}
                      </p>
                      <p className="text-[13px] text-[var(--color-text-muted)]">{latestAttempt.nextSteps}</p>
                    </div>
                  ) : null}
                  <p className="text-[11.5px] text-[var(--color-text-subtle)]">{t("practice.eduDisclaimer")}</p>
                  <button
                    type="button"
                    onClick={() => setMode("study")}
                    className="self-start text-[12.5px] font-semibold text-[var(--color-primary)] hover:underline"
                  >
                    {t("practice.linkBackToStudy")}
                  </button>
                </>
              )}
            </div>
          ) : null}
        </Card>
      )}

      <Card title={t("attempts.title")}>
        {attempts.length === 0 ? (
          <EmptyState>{t("attempts.emptyState")}</EmptyState>
        ) : (
          <ul className="flex flex-col gap-2">
            {attempts.map((attempt) => {
              const expanded = expandedAttempt === attempt.attemptNo;
              return (
                <li key={attempt.attemptNo} className="rounded-lg border border-[var(--color-border)]">
                  <button
                    type="button"
                    aria-expanded={expanded}
                    onClick={() => setExpandedAttempt(expanded ? null : attempt.attemptNo)}
                    className="flex w-full items-center gap-3 px-3 py-2.5 text-left"
                  >
                    <span className="text-[12.5px] font-semibold">
                      {t("attempts.attemptNo", { n: attempt.attemptNo })}
                    </span>
                    <span className="text-[12px] text-[var(--color-text-subtle)]">
                      {new Date(attempt.createdAt).toLocaleString("vi-VN")}
                    </span>
                    <span className="ml-auto">
                      <Badge variant={attempt.feedbackStatus === "completed" ? "success" : attempt.feedbackStatus === "pending" ? "blue" : "warn"}>
                        {t(`attempts.status.${attempt.feedbackStatus}`)}
                      </Badge>
                    </span>
                  </button>
                  {expanded ? (
                    <p className="border-t border-[var(--color-border)] px-3 py-2.5 text-[13px] whitespace-pre-line text-[var(--color-text-muted)]">
                      {attempt.answerText}
                    </p>
                  ) : null}
                </li>
              );
            })}
          </ul>
        )}
      </Card>
    </div>
  );
}
