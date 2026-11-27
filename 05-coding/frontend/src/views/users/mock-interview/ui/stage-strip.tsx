// The three-stage strip of the interview room, shared by the running room and the "opening" skeleton so the
// switch between them does not move anything. 02-bd/screens/users/USR0302_mock_interview.md (running header).
import { Check } from "lucide-react";
import type { InterviewStage } from "@/entities/mock-interview";
import { useT } from "@/shared/i18n";

export const STAGE_ORDER: InterviewStage[] = [
  "explain",
  "challenge",
  "scaleUp",
];

export function StageStrip({
  currentStage,
  turnLabel,
  clock,
  progress,
}: {
  currentStage: InterviewStage;
  /** "Lượt 1 / 10"; null while the session does not exist yet. */
  turnLabel: string | null;
  /** "00:33"; a running clock. */
  clock: string;
  /** 0 to 1, or null when the session has no turn ceiling to show. */
  progress: number | null;
}) {
  const t = useT("mockInterview");
  return (
    <div className="glass-card mb-3 border border-[var(--color-border)] px-4 py-3">
      <div className="flex flex-wrap items-center gap-2">
        {STAGE_ORDER.map((stage, index) => {
          const active = currentStage === stage;
          const done = STAGE_ORDER.indexOf(currentStage) > index;
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
        {turnLabel ? (
          <span className="ml-auto font-mono text-[12.5px] text-[var(--color-text-muted)]">
            {turnLabel}
          </span>
        ) : (
          <span className="ml-auto" />
        )}
        <span className="font-mono text-[12.5px] text-[var(--color-text-muted)]">
          {clock}
        </span>
      </div>
      {progress === null ? null : (
        <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-[var(--color-track)]">
          <div
            className="h-full rounded-full bg-[var(--color-primary)] transition-all"
            style={{ width: `${Math.min(100, progress * 100)}%` }}
          />
        </div>
      )}
    </div>
  );
}
