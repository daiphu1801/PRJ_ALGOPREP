// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 10.2 (USR0302_mock_interview).
//
// Layout follows 09-layoutBase/Phỏng vấn giả lập.dc.html: source tabs + session config
// (:104-196), a running header with a 3-stage progress strip + chat timeline (:202-340), and a result
// scorecard (:350-410). State machine lives in ./model/use-mock-interview-session.ts.
//
// Real SSE (F5-14) and real stage detection (F5-10..F5-12) do not exist — the hook scripts both
// deterministically and reveals AI replies with setInterval, matching what this task's brief asks
// for a PROTOTYPE lane ("mock streaming bằng cách giả lập gõ dần từng ký tự").
"use client";

import { useEffect, useRef, useState, type ReactNode } from "react";
import Link from "next/link";
import {
  Bot,
  Check,
  FileCode2,
  Flag,
  Lightbulb,
  MessageSquareText,
  Rocket,
  SendHorizontal,
  ShieldQuestion,
  Tags,
  User,
  type LucideIcon,
} from "lucide-react";
import {
  ACCEPTED_SUBMISSIONS,
  BANK_QUESTIONS,
  INTERVIEWER_LEVELS,
  MAX_TURNS_OPTIONS,
  RUBRIC_CRITERION_CODES,
  RUBRIC_WEIGHTS,
  type InterviewStage,
} from "@/entities/mock-interview";
import { useInterviewTopics } from "@/entities/interview-question";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  ConfirmDialog,
  SegmentedTabs,
  TextArea,
  Toggle,
} from "@/shared/ui";
import { useMockInterviewSession } from "../model/use-mock-interview-session";

// Per-message cap [SoT: 02-bd/screens/users/USR0302_mock_interview.md Sheet 5 khu vực F NO 1].
const MESSAGE_MAX_LENGTH = 4000;

const STAGE_ORDER: InterviewStage[] = ["explain", "challenge", "scaleUp"];

const STAGE_ICON: Record<InterviewStage, LucideIcon> = {
  explain: MessageSquareText,
  challenge: ShieldQuestion,
  scaleUp: Rocket,
};

const SOURCE_ICON: Record<string, LucideIcon> = {
  submission: FileCode2,
  bank: MessageSquareText,
  custom: Tags,
};

function scoreColor(score: number): string {
  if (score >= 8) return "var(--color-success)";
  if (score >= 5) return "var(--color-admin-warn)";
  return "var(--color-admin-negative)";
}

// One selectable source row (accepted submission or bank question) with a check on the chosen one.
function PickRow({
  icon: Icon,
  selected,
  onPick,
  title,
  meta,
}: {
  icon: LucideIcon;
  selected: boolean;
  onPick: () => void;
  title: string;
  meta?: ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onPick}
      aria-pressed={selected}
      className={
        "flex w-full items-center gap-3 rounded-xl border px-3.5 py-3 text-left text-[13.5px] transition-colors hover:bg-[var(--color-surface-hover)] " +
        (selected
          ? "border-[var(--color-primary)] bg-[color-mix(in_srgb,var(--color-primary)_8%,transparent)]"
          : "border-[var(--color-border)]")
      }
    >
      <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-[var(--color-surface-hover)] text-[var(--color-text-muted)]">
        <Icon className="h-4 w-4" aria-hidden="true" />
      </span>
      <span className="font-semibold">{title}</span>
      <span className="ml-auto flex items-center gap-2">
        {meta}
        {selected ? (
          <Check
            className="h-4 w-4 text-[var(--color-primary)]"
            aria-hidden="true"
          />
        ) : null}
      </span>
    </button>
  );
}

function formatElapsed(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}

export function MockInterviewView() {
  const t = useT("mockInterview");
  const session = useMockInterviewSession();
  const topicList = useInterviewTopics();
  const [draft, setDraft] = useState("");
  const [earlyExitOpen, setEarlyExitOpen] = useState(false);
  const chatEndRef = useRef<HTMLDivElement>(null);

  // Finishing (all turns used or stopped early) is reported once, whichever path got there.
  const reportedResult = useRef(false);
  useEffect(() => {
    if (session.screenState === "result" && !reportedResult.current)
      toast.success(t("toast.finished"));
    reportedResult.current = session.screenState === "result";
  }, [session.screenState, t]);

  useEffect(() => {
    // jsdom (used by the test suite) does not implement scrollIntoView.
    chatEndRef.current?.scrollIntoView?.({ block: "end" });
  }, [session.messages]);

  if (session.screenState === "entry") {
    const SourceIcon = SOURCE_ICON[session.entryType] ?? FileCode2;
    return (
      <section className="p-6">
        <div className="mb-5">
          <h1 className="text-lg font-bold tracking-tight">{t("title")}</h1>
          <p className="text-[12.5px] text-[var(--color-text-muted)]">
            {t("subtitle")}
          </p>
        </div>

        <div className="grid grid-cols-1 items-start gap-5 lg:[grid-template-columns:minmax(0,1fr)_340px]">
          <div className="flex min-w-0 flex-col gap-5">
            <div className="glass-card border border-[var(--color-border)] p-5">
              <div className="mb-4 flex flex-wrap items-center gap-3">
                <span className="text-[11.5px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
                  {t("startFrom")}
                </span>
                <SegmentedTabs
                  label={t("startFrom")}
                  value={session.entryType}
                  onValueChange={session.setEntryType}
                  options={[
                    { value: "submission", label: t("tab.submission") },
                    { value: "bank", label: t("tab.bank") },
                    { value: "custom", label: t("tab.custom") },
                  ]}
                />
              </div>

              {session.entryType === "submission" ? (
                <ul className="flex flex-col gap-2">
                  {ACCEPTED_SUBMISSIONS.map((submission) => (
                    <li key={submission.id}>
                      <PickRow
                        icon={SourceIcon}
                        selected={session.submissionId === submission.id}
                        onPick={() => session.setSubmissionId(submission.id)}
                        title={submission.problemTitle}
                        meta={
                          <Badge variant="neutral">{submission.language}</Badge>
                        }
                      />
                    </li>
                  ))}
                </ul>
              ) : null}

              {session.entryType === "bank" ? (
                <ul className="flex flex-col gap-2">
                  {BANK_QUESTIONS.map((question) => (
                    <li key={question.id}>
                      <PickRow
                        icon={SourceIcon}
                        selected={session.questionId === question.id}
                        onPick={() => session.setQuestionId(question.id)}
                        title={question.title}
                      />
                    </li>
                  ))}
                </ul>
              ) : null}

              {session.entryType === "custom" ? (
                <div>
                  <p className="mb-3 text-[13px] text-[var(--color-text-muted)]">
                    {t("customTopicHint")}
                  </p>
                  <div
                    role="group"
                    aria-label={t("customTopicsLabel")}
                    className="flex flex-wrap gap-2"
                  >
                    {topicList.map((topic) => {
                      const on = session.customTopics.includes(topic.key);
                      return (
                        <Button
                          key={topic.key}
                          variant={on ? "primary" : "ghost"}
                          size="sm"
                          aria-pressed={on}
                          className={
                            on
                              ? undefined
                              : "border border-[var(--color-border)]"
                          }
                          onClick={() => session.toggleCustomTopic(topic.key)}
                        >
                          {topic.label}
                        </Button>
                      );
                    })}
                  </div>
                </div>
              ) : null}
            </div>

            <div className="glass-card border border-[var(--color-border)] p-5">
              <h2 className="mb-4 text-[14.5px] font-bold">{t("howTitle")}</h2>
              <ol className="grid gap-4 sm:grid-cols-3">
                {STAGE_ORDER.map((stage, index) => {
                  const Icon = STAGE_ICON[stage];
                  return (
                    <li key={stage} className="flex gap-3">
                      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[color-mix(in_srgb,var(--color-primary)_14%,transparent)] text-[var(--color-primary)]">
                        <Icon className="h-4 w-4" aria-hidden="true" />
                      </span>
                      <div className="min-w-0">
                        <p className="text-[13.5px] font-semibold">
                          {index + 1}. {t(`stage.${stage}`)}
                        </p>
                        <p className="mt-0.5 text-[12.5px] text-[var(--color-text-muted)]">
                          {t(`how.${stage}`)}
                        </p>
                      </div>
                    </li>
                  );
                })}
              </ol>
            </div>
          </div>

          <aside className="glass-card border border-[var(--color-border)] p-5 lg:sticky lg:top-24">
            <h2 className="mb-4 text-[14.5px] font-bold">{t("configTitle")}</h2>

            <div className="mb-4">
              <p className="mb-1.5 text-xs font-medium text-[var(--color-text-muted)]">
                {t("levelLabel")}
              </p>
              <SegmentedTabs
                label={t("levelLabel")}
                value={session.level}
                onValueChange={session.setLevel}
                options={INTERVIEWER_LEVELS.map((level) => ({
                  value: level,
                  label: t(`level.${level}`),
                }))}
              />
              <p className="mt-2 text-[12.5px] text-[var(--color-text-muted)]">
                {t(`levelDesc.${session.level}`)}
              </p>
            </div>

            <div className="mb-4">
              <p className="mb-1.5 text-xs font-medium text-[var(--color-text-muted)]">
                {t("turnsLabel")}
              </p>
              <SegmentedTabs
                label={t("turnsLabel")}
                value={String(session.maxTurns)}
                onValueChange={(value) =>
                  session.setMaxTurns(
                    Number(value) as (typeof MAX_TURNS_OPTIONS)[number],
                  )
                }
                options={MAX_TURNS_OPTIONS.map((turns) => ({
                  value: String(turns),
                  label: t("turnsOption", { count: turns }),
                }))}
              />
            </div>

            <div className="mb-5 flex items-center justify-between rounded-xl border border-[var(--color-border)] px-3 py-2.5">
              <span className="flex items-center gap-2 text-[13px] font-semibold">
                <Lightbulb
                  className="h-4 w-4 text-[var(--color-admin-warn)]"
                  aria-hidden="true"
                />
                {t("hintLabel")}
              </span>
              <Toggle
                checked={session.hintAllowed}
                onCheckedChange={session.setHintAllowed}
                label={t("hintLabel")}
              />
            </div>

            <Button
              variant="cta"
              size="md"
              className="w-full"
              disabled={!session.canStart}
              onClick={() => {
                setDraft("");
                session.start();
              }}
            >
              <Rocket className="mr-2 h-4 w-4" aria-hidden="true" />
              {t("startCta")}
            </Button>
          </aside>
        </div>
      </section>
    );
  }

  if (session.screenState === "running") {
    const sourceName =
      session.entryType === "submission"
        ? ACCEPTED_SUBMISSIONS.find((s) => s.id === session.submissionId)
            ?.problemTitle
        : session.entryType === "bank"
          ? BANK_QUESTIONS.find((q) => q.id === session.questionId)?.title
          : topicList
              .filter((topic) => session.customTopics.includes(topic.key))
              .map((topic) => topic.label)
              .join(", ");
    return (
      // The chat fills the viewport (globals.css `[data-fullscreen-view]`); below `lg` it is a stacked page.
      <section
        data-fullscreen-view
        className="flex min-h-[80dvh] flex-col lg:min-h-0 lg:flex-1"
      >
        <div className="glass-card mb-3 border border-[var(--color-border)] px-4 py-3">
          <div className="flex flex-wrap items-center gap-2">
            {STAGE_ORDER.map((stage, index) => {
              const active = session.currentStage === stage;
              const done = STAGE_ORDER.indexOf(session.currentStage) > index;
              return (
                <span
                  key={stage}
                  aria-current={active ? "step" : undefined}
                  className={
                    "flex items-center gap-1.5 rounded-full border px-3 py-1 text-[12.5px] font-semibold " +
                    (active
                      ? "border-[var(--color-primary)] bg-[color-mix(in_srgb,var(--color-primary)_14%,transparent)] text-[var(--color-primary)]"
                      : done
                        ? "border-[var(--color-success)] text-[var(--color-success-text)]"
                        : "border-[var(--color-border)] text-[var(--color-text-subtle)]")
                  }
                >
                  {done ? (
                    <Check className="h-3.5 w-3.5" aria-hidden="true" />
                  ) : null}
                  {index + 1}. {t(`stage.${stage}`)}
                </span>
              );
            })}
            <span className="ml-auto font-mono text-[12.5px] text-[var(--color-text-muted)]">
              {t("turnCount", {
                current: session.turnCount,
                total: session.maxTurns,
              })}
            </span>
            <span className="font-mono text-[12.5px] text-[var(--color-text-muted)]">
              {formatElapsed(session.elapsedSeconds)}
            </span>
          </div>
          <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-[var(--color-track)]">
            <div
              className="h-full rounded-full bg-[var(--color-primary)] transition-all"
              style={{
                width: `${Math.min(100, (session.turnCount / session.maxTurns) * 100)}%`,
              }}
            />
          </div>
        </div>

        <div className="flex min-h-0 flex-1 flex-col gap-4 lg:flex-row">
          <div className="flex min-h-0 min-w-0 flex-1 flex-col">
            <div className="glass-card flex-1 overflow-y-auto border border-[var(--color-border)] p-4">
              <div
                role="log"
                aria-label={t("chatLogLabel")}
                aria-live="polite"
                aria-busy={session.isAiTyping}
                className="flex flex-col gap-4"
              >
                {session.messages.map((message) => {
                  const ai = message.role === "ai";
                  return (
                    <div
                      key={message.id}
                      className={
                        "flex max-w-[78%] items-end gap-2.5 " +
                        (ai ? "self-start" : "flex-row-reverse self-end")
                      }
                    >
                      <span
                        className={
                          "flex h-8 w-8 shrink-0 items-center justify-center rounded-full " +
                          (ai
                            ? "bg-[color-mix(in_srgb,var(--color-primary)_16%,transparent)] text-[var(--color-primary)]"
                            : "bg-[var(--color-surface-hover)] text-[var(--color-text-muted)]")
                        }
                        aria-hidden="true"
                      >
                        {ai ? (
                          <Bot className="h-4 w-4" />
                        ) : (
                          <User className="h-4 w-4" />
                        )}
                      </span>
                      <div className="min-w-0">
                        <p
                          className={
                            "mb-1 text-[11.5px] font-semibold text-[var(--color-text-subtle)] " +
                            (ai ? "" : "text-right")
                          }
                        >
                          {ai ? t("aiName") : t("youName")}
                        </p>
                        <div
                          className={
                            "rounded-2xl px-3.5 py-2.5 text-[13.5px] leading-relaxed whitespace-pre-wrap " +
                            (ai
                              ? "rounded-bl-md bg-[var(--color-surface-hover)]"
                              : "rounded-br-md bg-[var(--color-primary)] text-[var(--color-on-primary)]")
                          }
                        >
                          {message.content}
                        </div>
                      </div>
                    </div>
                  );
                })}
                {session.isAiTyping ? (
                  <span
                    role="status"
                    className="text-[12px] text-[var(--color-text-subtle)]"
                  >
                    {t("aiTyping")}
                  </span>
                ) : null}
                <div ref={chatEndRef} />
              </div>
            </div>

            <div className="glass-card mt-3 border border-[var(--color-border)] p-3 focus-within:border-[var(--color-primary)]">
              <TextArea
                label={t("messageInputLabel")}
                hideLabel
                value={draft}
                rows={3}
                readOnly={session.isAiTyping}
                maxLength={MESSAGE_MAX_LENGTH}
                onChange={(event) => setDraft(event.target.value)}
                onKeyDown={(event) => {
                  if (
                    event.key === "Enter" &&
                    (event.ctrlKey || event.metaKey) &&
                    !session.isAiTyping &&
                    draft.trim()
                  ) {
                    session.sendMessage(draft);
                    setDraft("");
                  }
                }}
                className="resize-none border-0 bg-transparent px-1 text-[14px] focus-visible:outline-none"
                placeholder={t("messageInputPlaceholder")}
              />
              <div className="mt-2 flex flex-wrap items-center gap-2 border-t border-[var(--color-border)] pt-2.5">
                {session.hintAllowed ? (
                  <Button
                    variant="ghost"
                    size="sm"
                    disabled={session.isAiTyping}
                    onClick={session.requestHint}
                    className="border border-[var(--color-border)]"
                  >
                    <Lightbulb
                      className="mr-1.5 h-4 w-4 text-[var(--color-admin-warn)]"
                      aria-hidden="true"
                    />
                    {t("hintCta")}
                  </Button>
                ) : null}
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setEarlyExitOpen(true)}
                  className="border border-[var(--color-border)] text-[var(--color-admin-negative-text)]"
                >
                  <Flag className="mr-1.5 h-4 w-4" aria-hidden="true" />
                  {t("stopEarlyCta")}
                </Button>
                <span className="ml-auto font-mono text-[11.5px] text-[var(--color-text-subtle)]">
                  {draft.length} / {MESSAGE_MAX_LENGTH} · {t("sendShortcut")}
                </span>
                <Button
                  variant="primary"
                  size="md"
                  disabled={session.isAiTyping || draft.trim().length === 0}
                  onClick={() => {
                    session.sendMessage(draft);
                    setDraft("");
                  }}
                >
                  <SendHorizontal className="mr-2 h-4 w-4" aria-hidden="true" />
                  {t("send")}
                </Button>
              </div>
            </div>
          </div>

          <aside className="flex flex-col gap-4 overflow-y-auto lg:w-[300px] lg:shrink-0">
            <div className="glass-card border border-[var(--color-border)] p-4">
              <h2 className="mb-3 text-[14px] font-bold">
                {t("sessionTitle")}
              </h2>
              <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-2 text-[13px]">
                <dt className="text-[var(--color-text-muted)]">
                  {t("sourceLabel")}
                </dt>
                <dd className="text-right font-semibold">{sourceName}</dd>
                <dt className="text-[var(--color-text-muted)]">
                  {t("levelLabel")}
                </dt>
                <dd className="text-right font-semibold">
                  {t(`level.${session.level}`)}
                </dd>
                <dt className="text-[var(--color-text-muted)]">
                  {t("turnsLeftLabel")}
                </dt>
                <dd className="text-right font-mono font-semibold">
                  {Math.max(0, session.maxTurns - session.turnCount)}
                </dd>
                <dt className="text-[var(--color-text-muted)]">
                  {t("hintLabel")}
                </dt>
                <dd className="text-right font-semibold">
                  {session.hintAllowed ? t("hintOn") : t("hintOff")}
                </dd>
              </dl>
            </div>
            <div className="glass-card border border-[var(--color-border)] p-4">
              <h2 className="mb-3 flex items-center gap-2 text-[14px] font-bold">
                <Lightbulb
                  className="h-4 w-4 text-[var(--color-admin-warn)]"
                  aria-hidden="true"
                />
                {t("tipsTitle")}
              </h2>
              <ul className="flex list-disc flex-col gap-2 pl-4 text-[12.5px] text-[var(--color-text-muted)]">
                <li>{t("tip1")}</li>
                <li>{t("tip2")}</li>
                <li>{t("tip3")}</li>
              </ul>
            </div>
          </aside>
        </div>

        <ConfirmDialog
          open={earlyExitOpen}
          onClose={() => setEarlyExitOpen(false)}
          onConfirm={() => {
            setEarlyExitOpen(false);
            setDraft("");
            session.stopEarly();
          }}
          title={t("earlyExitTitle")}
          confirmLabel={t("earlyExitConfirm")}
          cancelLabel={t("earlyExitCancel")}
          destructive
        >
          {t("earlyExitBody")}
        </ConfirmDialog>
      </section>
    );
  }

  if (!session.result) return null;

  const overall = session.result.overallScore;
  const tone = scoreColor(overall);
  return (
    <section className="p-6">
      <div className="glass-card mb-4 flex flex-wrap items-center gap-6 border border-[var(--color-border)] p-6">
        <div
          className="flex h-28 w-28 shrink-0 flex-col items-center justify-center rounded-full border-[6px]"
          style={{ borderColor: tone, color: tone }}
        >
          <span className="text-3xl leading-none font-bold">{overall}</span>
          <span className="text-[12px] font-semibold">/ 10</span>
        </div>
        <div className="min-w-0 flex-1">
          <p className="mb-1 text-xs font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
            {t("resultOverall")}
          </p>
          <p className="text-[13.5px] leading-relaxed text-pretty">
            {session.result.feedbackSummary}
          </p>
        </div>
      </div>

      <div className="mb-4 grid gap-3.5 sm:grid-cols-2">
        {RUBRIC_CRITERION_CODES.map((code) => {
          const criterion = session.result?.criteria.find(
            (c) => c.code === code,
          );
          if (!criterion) return null;
          return (
            <div
              key={code}
              className="glass-card border border-[var(--color-border)] p-4"
            >
              <div className="mb-2 flex items-baseline justify-between gap-2">
                <p className="text-[13.5px] font-semibold">
                  {t(`rubric.${code}`)} ({RUBRIC_WEIGHTS[code]}%)
                </p>
                <p className="font-mono text-sm font-semibold">
                  {criterion.score} / 10
                </p>
              </div>
              <div className="mb-2 h-1.5 overflow-hidden rounded-full bg-[var(--color-track)]">
                <div
                  className="h-full rounded-full"
                  style={{
                    width: `${criterion.score * 10}%`,
                    background: scoreColor(criterion.score),
                  }}
                />
              </div>
              <p className="text-[12.5px] text-[var(--color-text-muted)]">
                {criterion.comment}
              </p>
            </div>
          );
        })}
      </div>

      <div className="flex gap-3">
        <Button
          variant="cta"
          size="md"
          onClick={() => {
            setDraft("");
            session.retryPractice();
          }}
        >
          {t("retryCta")}
        </Button>
        <Button variant="ghost" size="md" asChild>
          <Link href="/problems">{t("backCta")}</Link>
        </Button>
      </div>
    </section>
  );
}
