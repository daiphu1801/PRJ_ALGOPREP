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

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, Bookmark } from "lucide-react";
import {
  useInterviewQuestion,
  topicLabel,
  levelLabel,
  useGradePracticeAnswer,
  RECALL_NEXT_REVIEW_DAYS,
  useInterviewLevels,
  useInterviewTopics,
  useRecallAndBookmarkState,
  RecallLevelPicker,
  type AnswerAttempt,
  type InterviewQuestion,
  type RecallLevel,
} from "@/entities/interview-question";
import { useLocale, useT } from "@/shared/i18n";
import { formatDateVN, type DateLocale } from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  NoticeTile,
  ProgressBar,
  QueryBoundary,
  SegmentedTabs,
  TextArea,
} from "@/shared/ui";

// 5000 characters, owner 2026-10-18 [SoT: 02-bd/screens/users/USR0402_interview_question_detail.md
// Câu hỏi mở Q3]. In production this is a server constant; the view keeps a copy for the counter.
const ANSWER_MAX_LENGTH = 5000;

type Mode = "study" | "practice";

// `key` remounts the screen when the route param changes, so the seeded question, attempts and
// composer never carry over from the previous question.
export function InterviewQuestionDetailView({
  questionId,
}: {
  questionId: string;
}) {
  return (
    <QueryBoundary query={useInterviewQuestion(questionId)}>
      {(question) => <QuestionDetail key={questionId} question={question} />}
    </QueryBoundary>
  );
}

function QuestionDetail({ question }: { question: InterviewQuestion | null }) {
  const t = useT("interviewQuestionDetail");
  const locale = useLocale() as DateLocale;
  const router = useRouter();
  const topicList = useInterviewTopics();
  const levelList = useInterviewLevels();
  const { mutateAsync: gradeAnswer } = useGradePracticeAnswer();

  // Hook cần một mảng — câu hỏi đơn lẻ vẫn seed đúng một khoá. Câu hỏi không tồn tại thì mảng rỗng,
  // hook vẫn an toàn (map rỗng).
  const { recall, bookmarks, rate, toggleBookmark } = useRecallAndBookmarkState(
    question ? [question] : [],
  );

  const [mode, setMode] = useState<Mode>("study");
  const [answerText, setAnswerText] = useState("");
  const [attempts, setAttempts] = useState<AnswerAttempt[]>(
    question?.attempts ?? [],
  );
  const [pendingFeedback, setPendingFeedback] = useState(false);
  // attemptNo whose feedback is on screen in Practice mode. Null = empty composer (always the state on
  // entry and after "Thử lại một lượt mới"); the composer is read-only only while this is set.
  const [shownAttemptNo, setShownAttemptNo] = useState<number | null>(null);
  const [expandedAttempt, setExpandedAttempt] = useState<number | null>(null);
  const [leaveTarget, setLeaveTarget] = useState<string | null>(null);
  // The simulated AI delay must not fire after the learner has left the screen.
  const feedbackTimer = useRef<number | null>(null);
  useEffect(
    () => () => {
      if (feedbackTimer.current !== null)
        window.clearTimeout(feedbackTimer.current);
    },
    [],
  );

  if (!question) {
    return (
      <div className="p-6">
        <Card>
          <EmptyState>{t("notFound")}</EmptyState>
          <div className="mt-3 text-center">
            <Link
              href="/interview-bank"
              className="text-[13px] font-semibold text-[var(--color-primary)] hover:underline"
            >
              {t("header.linkBack")}
            </Link>
          </div>
        </Card>
      </div>
    );
  }

  const shownAttempt =
    attempts.find((attempt) => attempt.attemptNo === shownAttemptNo) ?? null;
  const questionRecall = recall[question.code] ?? null;
  const bookmarked = bookmarks[question.code] ?? false;
  // Unsent text in the composer, for the first attempt and every later one alike.
  const hasDraft =
    mode === "practice" && answerText.trim().length > 0 && !shownAttempt;

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
    if (!trimmed || trimmed.length > ANSWER_MAX_LENGTH || pendingFeedback)
      return;
    setPendingFeedback(true);
    const attemptNo = Math.max(0, ...attempts.map((a) => a.attemptNo)) + 1;
    // Không gọi AI thật (prototype) — mô phỏng độ trễ và một phản hồi mẫu bám theo rubric của câu
    // hỏi, để khối phản hồi có nội dung liên quan thay vì text vô nghĩa cố định.
    // `question` is narrowed non-null by the early return above, but that narrowing does not
    // survive into a closure — `setTimeout`'s callback runs later, so TS re-widens it to
    // `| undefined` here. The value itself cannot actually change (seeded once via useState).
    const rubric = question!.rubric;
    feedbackTimer.current = window.setTimeout(async () => {
      let criteria;
      try {
        criteria = await gradeAnswer(rubric);
      } catch (error) {
        setPendingFeedback(false);
        toast.error(error instanceof Error ? error.message : String(error));
        return;
      }
      const strengths = rubric
        .slice(0, 1)
        .map((c) =>
          t("practice.feedbackSample.strength", { criterion: c.label }),
        );
      const gaps = rubric
        .slice(1, 2)
        .map((c) => t("practice.feedbackSample.gap", { criterion: c.label }));
      setAttempts((prev) => [
        {
          attemptNo,
          createdAt: new Date().toISOString(),
          feedbackStatus: "completed",
          answerText: trimmed,
          strengths,
          gaps,
          nextSteps: t("practice.feedbackSample.nextSteps"),
          criteria,
        },
        ...prev,
      ]);
      setShownAttemptNo(attemptNo);
      setPendingFeedback(false);
      toast.success(t("toast.answerSubmitted"));
    }, 900);
  }

  function rateWithToast(level: RecallLevel) {
    rate(question!.code, level);
    toast.success(t("toast.rated"));
  }

  function toggleBookmarkWithToast() {
    toggleBookmark(question!.code);
    toast.success(
      (bookmarks[question!.code] ?? false)
        ? t("toast.unbookmarked")
        : t("toast.bookmarked"),
    );
  }

  function retryPractice() {
    setAnswerText("");
    setShownAttemptNo(null);
  }

  return (
    <div className="p-6">
      <ConfirmDialog
        open={leaveTarget !== null}
        onClose={() => setLeaveTarget(null)}
        onConfirm={() => {
          const target = leaveTarget;
          setLeaveTarget(null);
          if (target) router.push(target);
        }}
        title={t("leaveConfirm.title")}
        confirmLabel={t("leaveConfirm.btnLeave")}
        cancelLabel={t("leaveConfirm.btnStay")}
        destructive
      >
        {t("leaveConfirm.message")}
      </ConfirmDialog>

      <header className="mb-5 flex flex-wrap items-center gap-x-3 gap-y-2 border-b border-[var(--color-border)] pb-4">
        <Badge variant="neutral">{topicLabel(topicList, question.topic)}</Badge>
        <span className="text-[13px] font-semibold text-[var(--color-text-muted)]">
          {levelLabel(levelList, question.level)}
        </span>
        <div className="ml-auto flex gap-2">
          <Button
            variant="ghost"
            size="md"
            aria-pressed={bookmarked}
            className={`border ${
              bookmarked
                ? "border-[var(--color-admin-warn)] bg-[color-mix(in_srgb,var(--color-admin-warn)_18%,transparent)] text-[var(--color-admin-warn-text)]"
                : "border-[var(--color-border)]"
            }`}
            onClick={toggleBookmarkWithToast}
          >
            <Bookmark
              className={`mr-2 h-4 w-4 ${bookmarked ? "fill-current" : ""}`}
              aria-hidden="true"
            />
            {bookmarked ? t("header.bookmarked") : t("header.bookmark")}
          </Button>
          <Button
            variant="ghost"
            size="md"
            className="border border-[var(--color-border)]"
            onClick={() => guardedNavigate("/interview-bank")}
          >
            <ArrowLeft className="mr-2 h-4 w-4" aria-hidden="true" />
            {t("header.linkBack")}
          </Button>
        </div>
      </header>

      <div className="grid grid-cols-1 items-start gap-6 lg:[grid-template-columns:minmax(0,1fr)_320px]">
        <div className="min-w-0">
          <Card className="mb-4 px-6 py-5">
            <h1 className="mb-3 text-2xl font-bold tracking-tight text-pretty">
              {question.question}
            </h1>
            <p className="text-[14px] leading-relaxed whitespace-pre-line text-[var(--color-text-muted)]">
              {question.content}
            </p>
          </Card>

          <SegmentedTabs
            label={t("mode.groupLabel")}
            value={mode}
            onValueChange={(next) => {
              // Practice stays clickable when locked so the click says why.
              if (next === "practice" && !question.hasRubric) {
                toast.warning(t("mode.practiceLockedReason"));
                return;
              }
              setMode(next);
            }}
            options={[
              { value: "study" as const, label: t("mode.study") },
              { value: "practice" as const, label: t("mode.practice") },
            ]}
            className="mb-4"
          />
          {mode === "study" ? (
            <Card className="mb-4 flex flex-col gap-5">
              {question.suggestedApproach.length > 0 ? (
                <div>
                  <p className="mb-2 text-[10.5px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
                    {t("study.outlineLabel")}
                  </p>
                  <ul className="flex flex-col gap-2">
                    {question.suggestedApproach.map((point, index) => (
                      <li
                        key={`${index}-${point}`}
                        className="flex gap-2.5 rounded-lg bg-[var(--color-surface-hover)] px-3 py-2.5 text-[13.5px] text-[var(--color-text-muted)]"
                      >
                        <span className="font-mono text-[11.5px] font-semibold text-[var(--color-primary)]">
                          {String(index + 1).padStart(2, "0")}
                        </span>
                        <span>{point}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              ) : null}

              {question.sampleAnswerFramework.trim() ? (
                <div>
                  <p className="mb-2 flex items-center gap-2 text-[10.5px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
                    {t("study.answerFrameworkLabel")}
                    {topicList.find((item) => item.key === question.topic)
                      ?.usesStarFramework ? (
                      <Badge variant="neutral">{t("study.starTag")}</Badge>
                    ) : null}
                  </p>
                  <p className="text-[13.5px] whitespace-pre-line text-[var(--color-text)]">
                    {question.sampleAnswerFramework}
                  </p>
                </div>
              ) : null}

              {question.followUps.length > 0 ? (
                <div>
                  <p className="mb-2 text-[10.5px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
                    {t("study.followUpsLabel")}
                  </p>
                  <ul className="flex flex-col gap-1.5">
                    {question.followUps.map((followUp) => (
                      <li
                        key={followUp}
                        className="flex gap-2 text-[13px] text-[var(--color-text-muted)]"
                      >
                        <span
                          aria-hidden="true"
                          className="shrink-0 text-[var(--color-text-subtle)]"
                        >
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
                  value={questionRecall}
                  onRate={rateWithToast}
                  labels={recallLabels}
                  groupLabel={t("study.recallLabel")}
                  className="mb-2"
                />
                {questionRecall ? (
                  <p className="text-[12px] text-[var(--color-text-subtle)]">
                    {t("study.nextReviewHint", {
                      n: RECALL_NEXT_REVIEW_DAYS[questionRecall],
                    })}
                  </p>
                ) : null}
              </div>
            </Card>
          ) : (
            <Card className="mb-4 flex flex-col gap-3">
              <TextArea
                label={t("practice.answerLabel")}
                value={answerText}
                onChange={(event) => {
                  const next = event.target.value;
                  if (next.length > ANSWER_MAX_LENGTH)
                    toast.warning(
                      t("toast.answerTooLong", { max: ANSWER_MAX_LENGTH }),
                    );
                  setAnswerText(next.slice(0, ANSWER_MAX_LENGTH));
                }}
                disabled={pendingFeedback || shownAttempt !== null}
                rows={8}
                placeholder={t("practice.answerPlaceholder")}
              />
              <div className="flex items-center justify-between gap-3">
                <span
                  className={
                    "text-[12px] " +
                    (answerText.length >= ANSWER_MAX_LENGTH
                      ? "font-semibold text-[var(--color-admin-warn)]"
                      : "text-[var(--color-text-subtle)]")
                  }
                >
                  {t("practice.charCounter", {
                    count: answerText.length,
                    max: ANSWER_MAX_LENGTH,
                  })}
                </span>
                {shownAttempt ? (
                  <Button
                    variant="ghost"
                    size="sm"
                    className="border border-[var(--color-border)]"
                    onClick={retryPractice}
                  >
                    {t("practice.btnRetry")}
                  </Button>
                ) : (
                  <Button
                    size="sm"
                    disabled={
                      !answerText.trim() ||
                      answerText.length > ANSWER_MAX_LENGTH ||
                      pendingFeedback
                    }
                    aria-busy={pendingFeedback || undefined}
                    onClick={submitAnswer}
                  >
                    {pendingFeedback
                      ? t("practice.pendingState")
                      : t("practice.btnSubmit")}
                  </Button>
                )}
              </div>

              {shownAttempt ? (
                <div className="mt-2 flex flex-col gap-3 border-t border-[var(--color-border)] pt-4">
                  {shownAttempt.feedbackStatus === "pending" ? (
                    <p
                      role="status"
                      className="text-[13px] text-[var(--color-text-muted)]"
                    >
                      {t("practice.pendingState")}
                    </p>
                  ) : shownAttempt.feedbackStatus === "failed" ? (
                    <NoticeTile
                      tone="warn"
                      title={t("practice.aiUnavailable")}
                    />
                  ) : (
                    <>
                      {shownAttempt.strengths?.length ? (
                        <div>
                          <p className="mb-1.5 text-[10.5px] font-semibold tracking-[0.08em] text-[var(--color-success-text)] uppercase">
                            {t("practice.strengthsLabel")}
                          </p>
                          <ul className="list-disc pl-5 text-[13px] text-[var(--color-text-muted)]">
                            {shownAttempt.strengths.map((item) => (
                              <li key={item}>{item}</li>
                            ))}
                          </ul>
                        </div>
                      ) : null}
                      {shownAttempt.gaps?.length ? (
                        <div>
                          <p className="mb-1.5 text-[10.5px] font-semibold tracking-[0.08em] text-[var(--color-admin-warn)] uppercase">
                            {t("practice.gapsLabel")}
                          </p>
                          <ul className="list-disc pl-5 text-[13px] text-[var(--color-text-muted)]">
                            {shownAttempt.gaps.map((item) => (
                              <li key={item}>{item}</li>
                            ))}
                          </ul>
                        </div>
                      ) : null}
                      {shownAttempt.criteria?.length ? (
                        <div>
                          <p className="mb-1.5 text-[10.5px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
                            {t("practice.criteriaLabel")}
                          </p>
                          <ul className="flex flex-col gap-2.5">
                            {shownAttempt.criteria.map((criterion) => (
                              <li key={criterion.criterionCode}>
                                <div className="mb-1 flex items-baseline justify-between gap-3 text-[13px]">
                                  <span className="font-semibold text-[var(--color-text)]">
                                    {criterion.criterionCode}
                                  </span>
                                  <span className="text-[var(--color-text-muted)]">
                                    {t("practice.criterionRow", {
                                      achievement: criterion.achievementPercent,
                                      weight: criterion.weightPercent,
                                    })}
                                  </span>
                                </div>
                                <ProgressBar
                                  value={criterion.achievementPercent}
                                  label={criterion.criterionCode}
                                  fill="var(--color-primary)"
                                  height={6}
                                />
                              </li>
                            ))}
                          </ul>
                        </div>
                      ) : null}
                      {shownAttempt.nextSteps ? (
                        <div>
                          <p className="mb-1.5 text-[10.5px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
                            {t("practice.nextStepsLabel")}
                          </p>
                          <p className="text-[13px] text-[var(--color-text-muted)]">
                            {shownAttempt.nextSteps}
                          </p>
                        </div>
                      ) : null}
                      <p className="text-[11.5px] text-[var(--color-text-subtle)]">
                        {t("practice.eduDisclaimer")}
                      </p>
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
        </div>

        <aside className="flex flex-col gap-4 lg:sticky lg:top-24">
          {mode === "study" && question.coreKeywords.length > 0 ? (
            <Card title={t("study.keywordsLabel")}>
              <div className="flex flex-wrap gap-1.5">
                {question.coreKeywords.map((keyword) => (
                  <Badge key={keyword} variant="neutral">
                    {keyword}
                  </Badge>
                ))}
              </div>
            </Card>
          ) : null}
          <Card title={t("attempts.title")}>
            {attempts.length === 0 ? (
              <EmptyState>{t("attempts.emptyState")}</EmptyState>
            ) : (
              <ul className="flex flex-col gap-2">
                {attempts.map((attempt) => {
                  const expanded = expandedAttempt === attempt.attemptNo;
                  return (
                    <li
                      key={attempt.attemptNo}
                      className="rounded-lg border border-[var(--color-border)]"
                    >
                      <button
                        type="button"
                        aria-expanded={expanded}
                        onClick={() =>
                          setExpandedAttempt(
                            expanded ? null : attempt.attemptNo,
                          )
                        }
                        className="flex w-full items-center gap-3 px-3 py-2.5 text-left"
                      >
                        <span className="text-[12.5px] font-semibold">
                          {t("attempts.attemptNo", { n: attempt.attemptNo })}
                        </span>
                        <span className="text-[12px] text-[var(--color-text-subtle)]">
                          {formatDateVN(attempt.createdAt, {
                            locale,
                            withTime: true,
                          })}
                        </span>
                        <span className="ml-auto">
                          <Badge
                            variant={
                              attempt.feedbackStatus === "completed"
                                ? "success"
                                : attempt.feedbackStatus === "pending"
                                  ? "blue"
                                  : "warn"
                            }
                          >
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
        </aside>
      </div>
    </div>
  );
}
