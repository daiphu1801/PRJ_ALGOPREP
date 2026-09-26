// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 10.2 (USR0302_mock_interview).
//
// Layout follows 09-layoutBase/Phỏng vấn giả lập.dc.html: entry stats + source tabs + session config
// (:104-196), a running header with a 3-stage progress strip + chat timeline (:202-340), and a result
// scorecard (:350-410). State machine lives in ./model/use-mock-interview-session.ts.
//
// Real SSE (F5-14) and real stage detection (F5-10..F5-12) do not exist — the hook scripts both
// deterministically and reveals AI replies with setInterval, matching what this task's brief asks
// for a PROTOTYPE lane ("mock streaming bằng cách giả lập gõ dần từng ký tự").
"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import {
  ACCEPTED_SUBMISSIONS,
  BANK_QUESTIONS,
  INTERVIEWER_LEVELS,
  MAX_TURNS_OPTIONS,
  RUBRIC_CRITERION_CODES,
  RUBRIC_WEIGHTS,
  fetchEntryStats,
  type InterviewStage,
} from "@/entities/mock-interview";
import { useT } from "@/shared/i18n";
import { Badge, Button, ConfirmDialog, SegmentedTabs, StatCard, TextArea, Toggle } from "@/shared/ui";
import { useMockInterviewSession } from "../model/use-mock-interview-session";

const STAGE_ORDER: InterviewStage[] = ["explain", "challenge", "scaleUp"];

function formatElapsed(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}

export function MockInterviewView() {
  const t = useT("mockInterview");
  const session = useMockInterviewSession();
  const stats = fetchEntryStats();
  const [draft, setDraft] = useState("");
  const [earlyExitOpen, setEarlyExitOpen] = useState(false);
  const chatEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    // jsdom (used by the test suite) does not implement scrollIntoView.
    chatEndRef.current?.scrollIntoView?.({ block: "end" });
  }, [session.messages]);

  if (session.screenState === "entry") {
    return (
      <section className="p-6">
        <div className="mb-4 grid gap-3.5 [grid-template-columns:repeat(auto-fit,minmax(190px,1fr))]">
          <StatCard label={t("statSessions")} value={String(stats.sessionsCompleted)} meta={t("statSessionsMeta")} />
          <StatCard
            label={t("statAverage")}
            value={stats.averageScore !== null ? `${stats.averageScore} / 10` : "- / 10"}
          />
          <StatCard
            label={t("statWeakest")}
            value={stats.weakestCriterion ? t(`rubric.${stats.weakestCriterion}`) : "-"}
          />
        </div>

        <div className="glass-card mb-4 border border-[var(--color-border)] p-5">
          <div className="mb-4 flex items-center gap-3">
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
            <ul className="mb-4 flex flex-col gap-2">
              {ACCEPTED_SUBMISSIONS.map((submission) => (
                <li key={submission.id}>
                  <button
                    type="button"
                    onClick={() => session.setSubmissionId(submission.id)}
                    aria-pressed={session.submissionId === submission.id}
                    className={
                      "flex w-full items-center gap-3 rounded-xl border px-3 py-2.5 text-left text-[13px] " +
                      (session.submissionId === submission.id
                        ? "border-[var(--color-primary)] bg-[var(--color-surface-hover)]"
                        : "border-[var(--color-border)]")
                    }
                  >
                    <span className="font-mono text-[11px] text-[var(--color-text-subtle)]">
                      {submission.id}
                    </span>
                    <span className="font-semibold">{submission.problemTitle}</span>
                    <Badge variant="neutral" className="ml-auto">
                      {submission.language}
                    </Badge>
                  </button>
                </li>
              ))}
            </ul>
          ) : null}

          {session.entryType === "bank" ? (
            <ul className="mb-4 flex flex-col gap-2">
              {BANK_QUESTIONS.map((question) => (
                <li key={question.id}>
                  <button
                    type="button"
                    onClick={() => session.setQuestionId(question.id)}
                    aria-pressed={session.questionId === question.id}
                    className={
                      "flex w-full items-center gap-3 rounded-xl border px-3 py-2.5 text-left text-[13px] " +
                      (session.questionId === question.id
                        ? "border-[var(--color-primary)] bg-[var(--color-surface-hover)]"
                        : "border-[var(--color-border)]")
                    }
                  >
                    <span className="font-mono text-[11px] text-[var(--color-text-subtle)]">
                      {question.id}
                    </span>
                    <span className="font-semibold">{question.title}</span>
                  </button>
                </li>
              ))}
            </ul>
          ) : null}

          {session.entryType === "custom" ? (
            <p className="mb-4 text-[13px] text-[var(--color-text-muted)]">{t("customTopicHint")}</p>
          ) : null}
        </div>

        <div className="glass-card mb-4 border border-[var(--color-border)] p-5">
          <h2 className="mb-3 text-[14.5px] font-bold">{t("configTitle")}</h2>

          <div className="mb-3">
            <p className="mb-1.5 text-xs font-medium text-[var(--color-text-muted)]">{t("levelLabel")}</p>
            <SegmentedTabs
              label={t("levelLabel")}
              value={session.level}
              onValueChange={session.setLevel}
              options={INTERVIEWER_LEVELS.map((level) => ({ value: level, label: t(`level.${level}`) }))}
            />
          </div>

          <div className="mb-3">
            <p className="mb-1.5 text-xs font-medium text-[var(--color-text-muted)]">{t("turnsLabel")}</p>
            <SegmentedTabs
              label={t("turnsLabel")}
              value={String(session.maxTurns)}
              onValueChange={(value) => session.setMaxTurns(Number(value) as (typeof MAX_TURNS_OPTIONS)[number])}
              options={MAX_TURNS_OPTIONS.map((turns) => ({
                value: String(turns),
                label: t("turnsOption", { count: turns }),
              }))}
            />
          </div>

          <div className="mb-4 flex items-center justify-between">
            <span className="text-[13px] font-semibold">{t("hintLabel")}</span>
            <Toggle checked={session.hintAllowed} onCheckedChange={session.setHintAllowed} label={t("hintLabel")} />
          </div>

          <Button variant="cta" size="md" className="w-full" disabled={!session.canStart} onClick={session.start}>
            {t("startCta")}
          </Button>
        </div>
      </section>
    );
  }

  if (session.screenState === "running") {
    return (
      <section className="flex h-[calc(100vh-32px)] flex-col p-6">
        <div className="glass-card mb-3 flex items-center gap-4 border border-[var(--color-border)] px-4 py-3">
          {STAGE_ORDER.map((stage, index) => (
            <span
              key={stage}
              className={
                "flex items-center gap-1.5 text-[12.5px] font-semibold " +
                (session.currentStage === stage
                  ? "text-[var(--color-primary)]"
                  : "text-[var(--color-text-subtle)]")
              }
            >
              {index + 1}. {t(`stage.${stage}`)}
            </span>
          ))}
          <span className="ml-auto font-mono text-[12.5px] text-[var(--color-text-muted)]">
            {t("turnCount", { current: session.turnCount, total: session.maxTurns })}
          </span>
          <span className="font-mono text-[12.5px] text-[var(--color-text-muted)]">
            {formatElapsed(session.elapsedSeconds)}
          </span>
        </div>

        <div className="glass-card flex-1 overflow-y-auto border border-[var(--color-border)] p-4">
          <div className="flex flex-col gap-3">
            {session.messages.map((message) => (
              <div
                key={message.id}
                className={
                  "max-w-[70%] rounded-2xl px-3.5 py-2.5 text-[13.5px] leading-relaxed whitespace-pre-wrap " +
                  (message.role === "ai"
                    ? "self-start bg-[var(--color-surface-hover)]"
                    : "self-end bg-[var(--color-primary)] text-[var(--color-on-primary)]")
                }
              >
                {message.content}
              </div>
            ))}
            {session.isAiTyping ? (
              <span className="text-[12px] text-[var(--color-text-subtle)]">{t("aiTyping")}</span>
            ) : null}
            <div ref={chatEndRef} />
          </div>
        </div>

        <div className="mt-3 flex items-end gap-2">
          <TextArea
            label={t("messageInputLabel")}
            hideLabel
            value={draft}
            rows={2}
            disabled={session.isAiTyping}
            onChange={(event) => setDraft(event.target.value)}
            wrapperClassName="flex-1"
            placeholder={t("messageInputPlaceholder")}
          />
          <Button
            variant="primary"
            size="md"
            disabled={session.isAiTyping || draft.trim().length === 0}
            onClick={() => {
              session.sendMessage(draft);
              setDraft("");
            }}
          >
            {t("send")}
          </Button>
          {session.hintAllowed ? (
            <Button variant="ghost" size="md" disabled={session.isAiTyping} onClick={session.requestHint}>
              {t("hintCta")}
            </Button>
          ) : null}
          <Button variant="ghost" size="md" onClick={() => setEarlyExitOpen(true)}>
            {t("stopEarlyCta")}
          </Button>
        </div>

        <ConfirmDialog
          open={earlyExitOpen}
          onClose={() => setEarlyExitOpen(false)}
          onConfirm={() => {
            setEarlyExitOpen(false);
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

  return (
    <section className="p-6">
      <div className="glass-card mb-4 border border-[var(--color-border)] p-6 text-center">
        <p className="mb-1 text-xs font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
          {t("resultOverall")}
        </p>
        <p className="text-4xl font-bold">{session.result.overallScore} / 10</p>
      </div>

      <div className="mb-4 grid gap-3.5 sm:grid-cols-2">
        {RUBRIC_CRITERION_CODES.map((code) => {
          const criterion = session.result?.criteria.find((c) => c.code === code);
          if (!criterion) return null;
          return (
            <div key={code} className="glass-card border border-[var(--color-border)] p-4">
              <p className="mb-1 text-[13.5px] font-semibold">
                {t(`rubric.${code}`)} ({RUBRIC_WEIGHTS[code]}%)
              </p>
              <p className="mb-1.5 font-mono text-sm font-semibold">{criterion.score} / 10</p>
              <p className="text-[12.5px] text-[var(--color-text-muted)]">{criterion.comment}</p>
            </div>
          );
        })}
      </div>

      <div className="glass-card mb-4 border border-[var(--color-border)] p-5">
        <p className="text-[13.5px] leading-relaxed text-pretty">{session.result.feedbackSummary}</p>
      </div>

      <div className="flex gap-3">
        <Button variant="cta" size="md" onClick={session.retryPractice}>
          {t("retryCta")}
        </Button>
        <Button variant="ghost" size="md" asChild>
          <Link href="/problems">{t("backCta")}</Link>
        </Button>
      </div>
    </section>
  );
}
