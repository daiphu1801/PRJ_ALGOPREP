// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { Suspense } from "react";
import { useT } from "@/shared/i18n";
import { ThemeLangSwitcher } from "@/shared/ui";
import type { AuthMode } from "@/entities/auth";
import { AuthForm, AuthLoadingOverlay, useAuthFlow } from "@/features/auth-by-credentials";
import { AuthAsidePanel } from "./auth-aside-panel";

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

        <AuthAsidePanel mode={mode} onSwitchMode={() => setMode(mode === "login" ? "signup" : "login")} />

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
