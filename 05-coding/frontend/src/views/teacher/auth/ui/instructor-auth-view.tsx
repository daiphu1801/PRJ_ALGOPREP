// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { Suspense } from "react";
import Link from "next/link";
import { useT } from "@/shared/i18n";
import { ThemeLangSwitcher } from "@/shared/ui";
import { instructorLogin } from "@/entities/auth";
import { AuthForm, AuthLoadingOverlay, useAuthFlow } from "@/features/auth-by-credentials";

/**
 * Dedicated Instructor login screen (`DEC-2026-0925-instructor-separate-login-route`) — a separate
 * route AND a separate backend endpoint (`instructorLogin`, `03-dd/api/identity.md` endpoint #49),
 * unlike `views/admin-auth` which only splits the route/skin and keeps the shared `login()` call.
 * Locked to `login` mode (no signup — instructor accounts are promoted by an admin, not
 * self-service, `01-rd/screens/admin/ADM0201_user_management.md:94`) and `showOAuth={false}` (same
 * open question as admin: linking an OAuth account through a role-restricted endpoint needs its own
 * design this decision doesn't cover).
 *
 * No `09-layoutBase` mockup exists for an instructor login screen (only Admin has one, which is why
 * `admin-auth-view` could borrow its cyan/teal palette) — per the project's "bám 09-layoutBase,
 * không bịa" rule, this screen reuses the shared `auth` screen's neutral `.glass-card` styling
 * instead of inventing a new color identity, distinguished only by its own heading/copy.
 */
function InstructorAuthViewContent() {
  const t = useT("auth");
  const tInstructor = useT("instructorAuth");
  const flow = useAuthFlow("login", instructorLogin);
  const { loadingStep } = flow;

  return (
    <div className="relative flex min-h-screen w-full items-center justify-center p-4">
      <ThemeLangSwitcher variant="floating" />

      <section className="glass-card relative w-full max-w-md p-8">
        <p className="text-xs font-medium uppercase tracking-wide text-[var(--color-text-muted)]">AlgoPrep</p>
        <h1 className="mt-2 text-2xl font-semibold text-[var(--color-text)]">{tInstructor("title")}</h1>
        <p className="mt-2 text-sm leading-relaxed text-[var(--color-text-muted)]">{tInstructor("subtitle")}</p>

        <div className="mt-6">
          <AuthForm flow={flow} showOAuth={false} />
        </div>

        <p className="mt-6 text-center text-xs text-[var(--color-text-muted)]">
          {tInstructor("notInstructorHint")}{" "}
          <Link href="/login" className="font-medium text-[var(--color-primary)] hover:underline">
            {t("loginTitle")}
          </Link>
        </p>

        {loadingStep !== null && <AuthLoadingOverlay currentStep={loadingStep} />}
      </section>
    </div>
  );
}

export function InstructorAuthView() {
  // useSearchParams (inside useAuthFlow) requires a Suspense boundary in the App Router.
  return (
    <Suspense fallback={null}>
      <InstructorAuthViewContent />
    </Suspense>
  );
}
