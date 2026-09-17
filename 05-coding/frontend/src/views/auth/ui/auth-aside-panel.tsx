// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useT } from "@/shared/i18n";
import type { AuthMode } from "@/entities/auth";

/**
 * Right column (`AsideModePanel`, 02-bd/screens/shared/auth.md section 2): badges, mode-dependent
 * copy, a static code illustration, nav dots. Page composition specific to this one screen (not
 * reused elsewhere) — kept as a views-local component per frontend_architecture.md section 2.A
 * ("dùng ở nhiều nơi KHÔNG phải tiêu chí" cuts both ways: a one-off composition stays in views/,
 * it doesn't need to earn a promotion to shared/features just because it's a separate file).
 */
export function AuthAsidePanel({ mode, onSwitchMode }: { mode: AuthMode; onSwitchMode: () => void }) {
  const t = useT("auth");
  const isSignup = mode === "signup" || mode === "login";
  const showSwitch = mode === "signup" || mode === "login";

  return (
    <aside className="hidden flex-col justify-between rounded-r-2xl bg-[var(--color-primary)] p-8 text-[var(--color-on-primary)] lg:flex">
      <div>
        <div className="mb-6 flex gap-2 text-xs font-medium">
          <span className="rounded-full bg-white/15 px-3 py-1">{t("badgeProblems")}</span>
          <span className="rounded-full bg-white/15 px-3 py-1">{t("badgeTopics")}</span>
          <span className="rounded-full bg-white/15 px-3 py-1">{t("badgeLanguages")}</span>
        </div>
        <h2 className="text-xl font-semibold">
          {mode === "login" ? t("asideLoginTitle") : t("asideSignupTitle")}
        </h2>
        <p className="mt-2 text-sm text-white/80">
          {mode === "login" ? t("asideLoginDescription") : t("asideSignupDescription")}
        </p>
        {showSwitch && (
          <button
            type="button"
            onClick={onSwitchMode}
            className="mt-4 rounded-md border border-white/40 px-3 py-1.5 text-xs font-medium hover:bg-white/10"
          >
            {mode === "login" ? t("switchToSignup") : t("switchToLogin")}
          </button>
        )}
      </div>

      {isSignup && (
        <pre className="mt-6 overflow-hidden rounded-lg bg-black/25 p-3 text-[11px] leading-relaxed text-white/70" aria-hidden="true">
          {"function twoSum(nums, target) {\n  const seen = new Map();\n  for (let i = 0; i < nums.length; i++) {\n    ...\n  }\n}"}
        </pre>
      )}

      <div className="mt-6 flex gap-1.5" aria-hidden="true">
        {[0, 1, 2].map((dot) => (
          <span
            key={dot}
            className="h-1.5 w-1.5 rounded-full"
            style={{ background: dot === 0 ? "white" : "rgba(255,255,255,0.4)" }}
          />
        ))}
      </div>
    </aside>
  );
}
