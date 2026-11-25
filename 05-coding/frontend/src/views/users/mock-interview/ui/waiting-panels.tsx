// What the learner sees while the AI works: opening a session (the interviewer writes the first question) and
// scoring it. Neither call reports progress, so there is NO percentage bar: the clock is the real elapsed time,
// the steps say only what is true (sent / being written / not here yet), and a hint of the usual wait comes from
// the durations this browser really measured.
"use client";

import { useEffect, useState } from "react";
import { Bot, Check, Loader2 } from "lucide-react";
import { RUBRIC_CRITERION_CODES } from "@/entities/mock-interview";
import { useT } from "@/shared/i18n";
import { Skeleton } from "@/shared/ui";
import { usualOpenSeconds } from "../model/open-duration";
import { StageStrip } from "./stage-strip";

/** Whole seconds since the component using it mounted (it only exists while the AI is working). Ticks once a second. */
export function useElapsedSeconds(): number {
  const [startedAt] = useState(() => Date.now());
  const [now, setNow] = useState(startedAt);
  useEffect(() => {
    const id = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(id);
  }, []);
  return Math.floor((now - startedAt) / 1000);
}

export function formatClock(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}

/** Past this the screen says it is slower than usual (it is never called an error). */
const SLOW_AFTER_SECONDS = 15;

type StepState = "done" | "active" | "todo";

function Step({ state, label }: { state: StepState; label: string }) {
  return (
    <li
      aria-current={state === "active" ? "step" : undefined}
      className={
        "flex items-start gap-2.5 text-[13px] " +
        (state === "todo"
          ? "text-[var(--color-text-subtle)]"
          : "text-[var(--color-text)]")
      }
    >
      <span
        aria-hidden="true"
        className={
          "mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full border " +
          (state === "done"
            ? "border-[var(--color-success)] bg-[var(--color-success)] text-[var(--color-on-primary)]"
            : state === "active"
              ? "border-[var(--color-primary)] text-[var(--color-primary)]"
              : "border-[var(--color-border)]")
        }
      >
        {state === "done" ? (
          <Check className="h-3 w-3" />
        ) : state === "active" ? (
          <Loader2 className="h-3 w-3 animate-spin" />
        ) : null}
      </span>
      <span className={state === "active" ? "font-semibold" : undefined}>
        {label}
      </span>
    </li>
  );
}

/**
 * The room is drawn at once (same frame as the running room, so nothing jumps when the first question lands)
 * with a skeleton where the interviewer's message will be.
 */
export function OpeningRoom({ turnLimit }: { turnLimit: number | null }) {
  const t = useT("mockInterview");
  const elapsed = useElapsedSeconds();
  const usual = usualOpenSeconds();
  return (
    <section
      data-fullscreen-view
      aria-busy="true"
      className="flex min-h-[calc(80*var(--dvh1))] flex-col lg:min-h-0 lg:flex-1"
    >
      <StageStrip
        currentStage="explain"
        turnLabel={
          turnLimit === null
            ? null
            : t("turnCount", { current: 0, total: turnLimit })
        }
        clock={formatClock(elapsed)}
        progress={null}
      />
      <div className="flex min-h-0 flex-1 flex-col gap-4 lg:flex-row">
        <div className="glass-card min-h-[320px] min-w-0 flex-1 border border-[var(--color-border)] p-4">
          <div className="flex max-w-[78%] items-end gap-2.5">
            <span
              aria-hidden="true"
              className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[color-mix(in_srgb,var(--color-primary)_16%,transparent)] text-[var(--color-primary)]"
            >
              <Bot className="h-4 w-4" />
            </span>
            <div className="min-w-0 flex-1">
              <p className="mb-1 text-[11.5px] font-semibold text-[var(--color-text-subtle)]">
                {t("aiName")}
              </p>
              <div className="flex w-[min(34rem,70vw)] flex-col gap-2 rounded-2xl rounded-bl-md bg-[var(--color-surface-hover)] px-3.5 py-3">
                <Skeleton className="h-3.5 w-full bg-[var(--color-border)]" />
                <Skeleton className="h-3.5 w-[92%] bg-[var(--color-border)]" />
                <Skeleton className="h-3.5 w-[64%] bg-[var(--color-border)]" />
              </div>
              <p
                role="status"
                className="mt-2 text-[12px] text-[var(--color-text-subtle)]"
              >
                {t("opening.composing")}
              </p>
            </div>
          </div>
        </div>
        <aside className="flex flex-col gap-4 lg:w-[300px] lg:shrink-0">
          <div className="glass-card border border-[var(--color-border)] p-4">
            <h2 className="mb-3 text-[14px] font-bold">{t("opening.title")}</h2>
            <ol className="flex flex-col gap-3">
              <Step state="done" label={t("opening.stepSent")} />
              <Step state="active" label={t("opening.stepWriting")} />
              <Step state="todo" label={t("opening.stepEnter")} />
            </ol>
            <p
              aria-hidden="true"
              className="mt-4 font-mono text-[12.5px] text-[var(--color-text-muted)]"
            >
              {t("opening.elapsed", { time: formatClock(elapsed) })}
              {usual === null
                ? ""
                : ` · ${t("opening.usual", { seconds: usual })}`}
            </p>
            {elapsed >= SLOW_AFTER_SECONDS ? (
              <p
                role="status"
                className="mt-2 text-[12.5px] text-[var(--color-text-muted)]"
              >
                {t("opening.slow")}
              </p>
            ) : null}
          </div>
        </aside>
      </div>
    </section>
  );
}

/** Shown in the chat while the AI scores the finished session: the four criteria it is about to grade. */
export function ScoringPanel() {
  const t = useT("mockInterview");
  const elapsed = useElapsedSeconds();
  return (
    <div
      role="status"
      className="rounded-xl border border-[var(--color-primary)] bg-[color-mix(in_srgb,var(--color-primary)_8%,transparent)] p-4"
    >
      <div className="flex items-center gap-2.5">
        <Loader2
          className="h-4 w-4 animate-spin text-[var(--color-primary)]"
          aria-hidden="true"
        />
        <p className="text-[13.5px] font-semibold">{t("scoring.title")}</p>
        <span
          aria-hidden="true"
          className="ml-auto font-mono text-[12.5px] text-[var(--color-text-muted)]"
        >
          {formatClock(elapsed)}
        </span>
      </div>
      <p className="mt-1 text-[12.5px] text-[var(--color-text-muted)]">
        {t("scoring.body")}
      </p>
      <ul className="mt-3 grid gap-2 sm:grid-cols-2" aria-hidden="true">
        {RUBRIC_CRITERION_CODES.map((code) => (
          <li
            key={code}
            className="rounded-lg border border-[var(--color-border)] px-3 py-2"
          >
            <p className="mb-1.5 text-[12px] font-semibold">
              {t(`rubric.${code}`)}
            </p>
            <Skeleton className="h-1.5 w-full" />
          </li>
        ))}
      </ul>
    </div>
  );
}
