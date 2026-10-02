// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { Suspense } from "react";
import { useT } from "@/shared/i18n";
import { AuthAsidePanel, ThemeLangSwitcher } from "@/shared/ui";
import type { AuthMode } from "@/entities/auth";
import { AuthForm, AuthLoadingOverlay, DevQuickLogin, useAuthFlow } from "@/features/auth-by-credentials";

export type { AuthMode };

type AuthViewProps = {
  /** Enables deep-linking: /login and /register land on the right mode, even though the UI is one screen. */
  initialMode?: AuthMode;
};

function AuthViewContent({ initialMode = "signup" }: AuthViewProps) {
  const t = useT("auth");
  const flow = useAuthFlow(initialMode);
  const { mode, setMode, loadingStep } = flow;

  const isLoginLike = mode === "login" || mode === "signup";
  const title = {
    signup: t("signupTitle"),
    login: t("loginTitle"),
    forgot_email: t("forgotEmailTitle"),
    forgot_otp: t("forgotOtpTitle"),
    forgot_reset: t("forgotResetTitle"),
  }[mode];

  return (
    <>
      <ThemeLangSwitcher variant="floating" />
      <section className="glass-surface relative grid w-full max-w-3xl grid-cols-1 overflow-hidden rounded-2xl border border-[var(--color-border)] shadow-lg lg:grid-cols-[1.06fr_1fr]">
        <div className="p-8">
          <p className="text-xs font-medium uppercase tracking-wide text-[var(--color-text-muted)]">AlgoPrep</p>
          <h1 className="mt-2 text-2xl font-semibold text-[var(--color-text)]">{title}</h1>
          <div className="mt-6">
            <AuthForm flow={flow} />
          </div>
          {/* All three roles here: this screen calls the shared `login()`, which routes by the role
              the mock returns, so it is the one entry point that can reach every area. */}
          <DevQuickLogin flow={flow} roles={["STUDENT", "INSTRUCTOR", "ADMIN"]} />
          {isLoginLike && (
            <button
              type="button"
              onClick={() => setMode(mode === "login" ? "signup" : "login")}
              className="mt-4 text-sm text-[var(--color-primary)] hover:underline lg:hidden"
            >
              {mode === "login" ? t("switchToSignup") : t("switchToLogin")}
            </button>
          )}
        </div>

        <AuthAsidePanel
          className="rounded-r-2xl"
          badges={[t("badgeProblems"), t("badgeTopics"), t("badgeLanguages")]}
          title={mode === "login" ? t("asideLoginTitle") : t("asideSignupTitle")}
          description={mode === "login" ? t("asideLoginDescription") : t("asideSignupDescription")}
          action={
            isLoginLike && (
              <button
                type="button"
                onClick={() => setMode(mode === "login" ? "signup" : "login")}
                className="on-accent-outline rounded-md border px-3 py-1.5 text-xs font-medium"
              >
                {mode === "login" ? t("switchToSignup") : t("switchToLogin")}
              </button>
            )
          }
        >
          {/* The mockup's illustration (09-layoutBase/Đăng nhập & Đăng ký.dc.html:165-171): a snippet
              of a solved problem, so the panel shows what the product is rather than describing it. */}
          <pre
            aria-hidden="true"
            className="on-accent-tint overflow-hidden rounded-lg p-3 text-[11px] leading-relaxed opacity-90"
          >
            {"function twoSum(nums, target) {\n  const seen = new Map();\n  for (let i = 0; i < nums.length; i++) {\n    ...\n  }\n}"}
          </pre>
        </AuthAsidePanel>

        {loadingStep !== null && <AuthLoadingOverlay currentStep={loadingStep} />}
      </section>
    </>
  );
}

export function AuthView(props: AuthViewProps) {
  // useSearchParams (inside useAuthFlow) requires a Suspense boundary in the App Router.
  return (
    <Suspense fallback={null}>
      <AuthViewContent {...props} />
    </Suspense>
  );
}
