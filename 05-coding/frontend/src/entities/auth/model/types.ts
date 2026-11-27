// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md

/** Mode of the `auth` screen state machine (02-bd/screens/shared/SHR0101_auth.md Sheet 3). */
export type AuthMode =
  | "signup"
  | "verify_email"
  | "login"
  | "forgot_email"
  | "forgot_otp"
  | "forgot_reset";

/**
 * Duplicated from entities/user/model/types.ts's `Role`, NOT imported from it — FSD forbids
 * entities importing other entities (eslint-plugin-boundaries: `entities` may only depend on
 * `shared`, per SYS0102_frontend_architecture.md section 2 "Quy tắc vàng"). Both must stay in sync with
 * README.md section 4, F1; a features/-layer mapper is the mechanical place to convert between
 * the two once a real API exists, not a shortcut import here.
 */
export type Role = "STUDENT" | "INSTRUCTOR" | "ADMIN";

/**
 * Result of a submit attempt against the mock backend. A discriminated union instead of throwing
 * for field-level errors, because BD Q1 locked in inline-per-field errors, not a toast/modal
 * (01-rd/screens/shared/auth.md:87) — the caller needs the error shape to route it per field.
 */
export type AuthFieldErrors = Record<string, string>;

export type AuthOutcome =
  | { ok: true; role: Role; deactivated?: false }
  // `deactivated-recovery` is a variant of `login`, not a separate mode
  // (02-bd/screens/shared/SHR0101_auth.md Sheet 3) — the caller stays on `login` and shows a banner.
  // No session is issued, so there is no role. The real backend (IDT-104) hands over a one-use
  // `restoreTicket` valid for 5 minutes; it is kept in memory only, never in the URL or storage.
  | { ok: true; deactivated: true; restoreTicket?: string }
  // `needsEmailVerification`: the account exists but its email was never confirmed (IDT-113), so
  // the caller moves to `verify_email` instead of only marking a field.
  | {
      ok: false;
      fieldErrors: AuthFieldErrors;
      needsEmailVerification?: boolean;
    };

/** Result of `signup`: the account is created pending, the next step is the emailed code. */
export type SignupOutcome =
  { ok: true; email: string } | { ok: false; fieldErrors: AuthFieldErrors };

export type ForgotEmailOutcome = { ok: true };

// `attemptsLeft` comes from the mock only; the real backend never reveals how many tries remain
// (DD F1 #60), so the flow counts down locally when it is absent.
export type ForgotOtpOutcome =
  | { ok: true }
  | { ok: false; fieldErrors: AuthFieldErrors; attemptsLeft?: number };

export type ForgotResetOutcome =
  { ok: true } | { ok: false; fieldErrors: AuthFieldErrors };
