// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { Suspense } from "react";
import Link from "next/link";
import { useT } from "@/shared/i18n";
import { LiquidGlassBackdrop, ThemeLangSwitcher } from "@/shared/ui";
import { AuthForm, AuthLoadingOverlay, useAuthFlow } from "@/features/auth-by-credentials";

/**
 * Dedicated Admin login screen (`DEC-2026-0915-admin-separate-login-route`) — a separate route
 * from the shared `auth` screen (`views/auth`), not a re-skin of it. Reuses the exact same
 * `useAuthFlow`/`loginSchema`/`login()` mutation from `entities/auth` (same backend contract), but
 * locked to the `login` mode (no signup) and `showOAuth={false}` (admin accounts are provisioned,
 * not self-service). `forgot_email` → `forgot_otp` → `forgot_reset` still work here — admins need
 * password recovery too, and `AuthForm` already carries that state machine.
 *
 * Styled with the Admin liquid-glass palette instead of the shared screen's neutral glass: the
 * root wrapper carries the `.admin-shell` CSS scope (globals.css) so `LiquidGlassBackdrop`,
 * `.glass-card`, `.glass-surface`, and `ThemeLangSwitcher` all pick up the cyan/teal tokens and
 * blob background from 09-layoutBase/Admin - Tổng quan.dc.html with zero component-level changes —
 * the same scope-override mechanism `AppShell` already uses for `area === "admin"`.
 */
function AdminAuthViewContent() {
  const t = useT("auth");
  const tAdmin = useT("adminAuth");
  const flow = useAuthFlow("login");
  const { mode, loadingStep } = flow;

  const title = {
    signup: t("signupTitle"),
    login: tAdmin("title"),
    forgot_email: t("forgotEmailTitle"),
    forgot_otp: t("forgotOtpTitle"),
    forgot_reset: t("forgotResetTitle"),
  }[mode];

  return (
    <div className="admin-shell relative flex min-h-screen w-full items-center justify-center overflow-hidden bg-[var(--color-background)] p-4 text-[var(--color-text)]">
      <LiquidGlassBackdrop />
      <ThemeLangSwitcher variant="floating" />

      {/* Soft cyan/teal halo directly behind the card — reads as the card catching light off the
          blob backdrop, reinforcing the floating-glass depth on top of the elevated shadow below.
          Purely decorative (aria-hidden), sized bigger than the card so the blur falls off past its
          edges instead of showing a hard-edged glow rectangle. */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute z-0 h-[420px] w-[420px] max-w-[90vw] rounded-full blur-[90px]"
        style={{ background: "radial-gradient(circle, var(--color-admin-cyan), transparent 70%)", opacity: 0.25 }}
      />

      <section
        className="glass-card glass-card--elevated relative z-10 w-full max-w-md p-8"
        style={{ border: "1px solid color-mix(in srgb, white 35%, var(--color-border))" }}
      >
        <div className="mb-6 flex items-center gap-2.5">
          <span
            aria-hidden="true"
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-sm font-extrabold"
            style={{ background: "var(--admin-logo-bg)", color: "var(--admin-logo-fg)" }}
          >
            A
          </span>
          <span className="text-base font-extrabold tracking-tight">AlgoPrep</span>
        </div>

        {mode === "login" && (
          <p className="mb-2 text-[10.5px] font-semibold uppercase tracking-[0.16em] text-[var(--color-admin-cyan)]">
            {tAdmin("eyebrow")}
          </p>
        )}
        <h1 className="text-2xl font-bold text-[var(--color-text)]">{title}</h1>
        {mode === "login" && (
          <p className="mt-2 text-sm leading-relaxed text-[var(--color-text-muted)]">{tAdmin("subtitle")}</p>
        )}

        <div className="mt-6">
          <AuthForm flow={flow} showOAuth={false} />
        </div>

        {mode === "login" && (
          <p className="mt-6 text-center text-xs text-[var(--color-text-muted)]">
            {tAdmin("notAdminHint")}{" "}
            <Link href="/login" className="font-medium text-[var(--color-admin-cyan)] hover:underline">
              {t("loginTitle")}
            </Link>
          </p>
        )}

        {loadingStep !== null && <AuthLoadingOverlay currentStep={loadingStep} />}
      </section>
    </div>
  );
}

export function AdminAuthView() {
  // useSearchParams (inside useAuthFlow) requires a Suspense boundary in the App Router.
  return (
    <Suspense fallback={null}>
      <AdminAuthViewContent />
    </Suspense>
  );
}
