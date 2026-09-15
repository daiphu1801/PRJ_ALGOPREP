// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useT } from "@/shared/i18n";
import { LOADING_STEP_COUNT } from "../model/use-auth-flow";

/**
 * 4 sequential steps with a mark (·/→/tick) — 02-bd/screens/shared/auth.md section 1 and section 2
 * (`AuthLoadingOverlay`). Not a generic spinner: the RD/BD explicitly designed this as a fixed
 * 4-step sequence, not a plain network-wait indicator.
 */
export function AuthLoadingOverlay({ currentStep }: { currentStep: number }) {
  const t = useT("auth");
  const steps = [t("loadingStep1"), t("loadingStep2"), t("loadingStep3"), t("loadingStep4")];

  return (
    <div
      role="status"
      aria-live="polite"
      className="glass-surface absolute inset-0 z-40 flex flex-col items-center justify-center gap-4 rounded-[inherit]"
    >
      <p className="text-sm font-semibold text-[var(--color-text)]">{t("loadingTitle")}</p>
      <div className="h-1.5 w-48 overflow-hidden rounded-full bg-[var(--color-surface-hover)]">
        <div
          className="h-full rounded-full bg-[var(--color-primary)] transition-all"
          style={{ width: `${((currentStep + 1) / LOADING_STEP_COUNT) * 100}%` }}
        />
      </div>
      <ul className="w-56 space-y-1.5 text-sm">
        {steps.map((label, index) => {
          // No pictographic check mark (CLAUDE.md "NO EMOJI OR PICTOGRAPHIC ICONS") — a completed
          // step says so in words instead of a tick glyph; `→`/`·` stay because the no-emoji rule
          // explicitly allows arrows-as-operator and typographic punctuation.
          const isDone = index < currentStep;
          const mark = isDone ? t("loadingStepDone") : index === currentStep ? "→" : "·";
          return (
            <li
              key={label}
              className={
                index <= currentStep ? "text-[var(--color-text)]" : "text-[var(--color-text-muted)]"
              }
            >
              <span className="mr-2 text-xs" aria-hidden="true">
                {mark}
              </span>
              {label}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
