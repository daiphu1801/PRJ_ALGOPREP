// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Zod schemas for the `auth` screen, one per mode of the state machine
// (02-bd/screens/shared/auth.md section 3). Keeping the schema here (not inline in the feature)
// is what makes the future swap to a real 03-dd/api/identity.md contract mechanical: the DD's
// client validation table becomes a diff on this file, not a rewrite.
import { z } from "zod";

// Username/password rules are NOT locked in yet (03-dd/validation/identity.md doesn't exist) —
// these are prototype-only placeholders, loose enough to let the mock respond, not a security
// policy. [SoT: Suy luận]
// Message values are i18n keys RELATIVE to the "auth" namespace (not full paths) — the feature
// layer translates them with useT("auth") before display, see features/auth-by-credentials/ui/auth-form.tsx.
const usernameField = z.string().min(3, "errors.usernameTooShort").max(32);
const passwordField = z.string().min(8, "errors.passwordTooShort");
const emailField = z.string().email("errors.emailInvalid");

export const signupSchema = z
  .object({
    username: usernameField,
    password: passwordField,
    email: emailField,
    termsAccepted: z.boolean(),
  })
  // BD Q1 (open question, 02-bd/screens/shared/auth.md:140): the prototype only has a toggle, no
  // validation. We resolve it for the prototype by blocking submit — matches the BD's own
  // recommendation ("Chặn submit, hiện cảnh báo dưới checkbox"), flagged as [SoT: Suy luận] until
  // DD confirms.
  .refine((value) => value.termsAccepted, {
    message: "errors.termsRequired",
    path: ["termsAccepted"],
  });

export const loginSchema = z.object({
  identifier: z.string().min(1, "errors.identifierRequired"),
  password: z.string().min(1, "errors.passwordRequired"),
  rememberMe: z.boolean(),
});

export const forgotEmailSchema = z.object({
  email: emailField,
});

export const forgotOtpSchema = z.object({
  otp: z.string().length(6, "errors.otpLength").regex(/^\d+$/, "errors.otpDigitsOnly"),
});

export const forgotResetSchema = z
  .object({
    newPassword: passwordField,
    confirmPassword: z.string(),
  })
  .refine((value) => value.newPassword === value.confirmPassword, {
    message: "errors.passwordMismatch",
    path: ["confirmPassword"],
  });

export type SignupInput = z.infer<typeof signupSchema>;
export type LoginInput = z.infer<typeof loginSchema>;
export type ForgotEmailInput = z.infer<typeof forgotEmailSchema>;
export type ForgotOtpInput = z.infer<typeof forgotOtpSchema>;
export type ForgotResetInput = z.infer<typeof forgotResetSchema>;
