// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Zod schemas for the `auth` screen, one per mode of the state machine
// (02-bd/screens/shared/SHR0101_auth.md Sheet 3). Keeping the schema here (not inline in the feature)
// is what makes the future swap to a real 03-dd/api/F1_identity.md contract mechanical: the DD's
// client validation table becomes a diff on this file, not a rewrite.
import { z } from "zod";
import { fitsBcrypt } from "@/shared/lib";

// Rules mirror the backend (03-dd/validation/F1_identity.md): display name 2 to 100 characters;
// password 8 characters minimum, at most 72 UTF-8 bytes (BCrypt limit,
// DEC-2026-1030-bcrypt-password-byte-limit), at least one letter and one digit.
// Message values are i18n keys RELATIVE to the "auth" namespace (not full paths) — the feature
// layer translates them with useT("auth") before display, see features/auth-by-credentials/ui/auth-form.tsx.
const displayNameField = z
  .string()
  .trim()
  .min(2, "errors.displayNameTooShort")
  .max(100, "errors.displayNameTooLong");
const passwordField = z
  .string()
  .min(8, "errors.passwordTooShort")
  .regex(/\p{L}/u, "errors.passwordNeedsLetter")
  .regex(/\d/, "errors.passwordNeedsDigit")
  .refine(fitsBcrypt, "errors.passwordTooLong");
const emailField = z.string().trim().email("errors.emailInvalid");

export const signupSchema = z
  .object({
    displayName: displayNameField,
    password: passwordField,
    email: emailField,
    termsAccepted: z.boolean(),
  })
  // BD Q1 (closed, 02-bd/screens/shared/SHR0101_auth.md, Câu hỏi mở): the prototype only has a toggle, no
  // validation. We resolve it for the prototype by blocking submit — matches the BD's own
  // recommendation ("Chặn submit, hiện cảnh báo dưới checkbox"), flagged as [SoT: Suy luận] until
  // DD confirms.
  .refine((value) => value.termsAccepted, {
    message: "errors.termsRequired",
    path: ["termsAccepted"],
  });

// Login matches by e-mail only: the backend has no username (BD Sheet 5 item 7, owner decision
// 2026-10-31). The password is not re-validated here, the server judges it (IDT-102).
export const loginSchema = z.object({
  email: emailField,
  password: z.string().min(1, "errors.passwordRequired"),
});

export const forgotEmailSchema = z.object({
  email: emailField,
});

export const forgotOtpSchema = z.object({
  otp: z
    .string()
    .length(6, "errors.otpLength")
    .regex(/^\d+$/, "errors.otpDigitsOnly"),
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
