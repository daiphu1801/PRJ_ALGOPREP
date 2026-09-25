// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Zod schemas for editing `UserProfile`, limits ported from 03-dd/validation/identity.md (the
// module's own validation DD, written today) rather than invented — this is the one screen slice
// in this run that already had a real DD to read before coding.
import { z } from "zod";

const displayNameField = z.string().trim().min(1, "errors.displayNameRequired").max(100, "errors.displayNameTooLong");
// `school_or_company` / `current_position` / `target_position` are optional free text — empty
// string means "not set" (server stores NULL) [SoT: 03-dd/validation/identity.md mục 5].
const schoolOrCompanyField = z.string().trim().max(150, "errors.schoolOrCompanyTooLong");
const currentPositionField = z.string().trim().max(100, "errors.currentPositionTooLong");
const targetPositionField = z.string().trim().max(100, "errors.targetPositionTooLong");

export const profileFormSchema = z.object({
  displayName: displayNameField,
  schoolOrCompany: schoolOrCompanyField,
  currentPosition: currentPositionField,
  defaultLanguage: z.enum(["JAVA", "CPP", "PYTHON"]),
  targetPosition: targetPositionField,
});

export type ProfileFormInput = z.infer<typeof profileFormSchema>;

// Password rule table [SoT: 03-dd/validation/identity.md mục 1]: 8-128 chars, at least one letter
// and one digit. Duplicated from entities/auth's own password rule rather than imported — same FSD
// reason entities/auth already documents for duplicating `Role` (entities cannot import entities).
const newPasswordField = z
  .string()
  .min(8, "errors.passwordTooShort")
  .max(128, "errors.passwordTooLong")
  .regex(/[A-Za-z]/, "errors.passwordNeedsLetter")
  .regex(/\d/, "errors.passwordNeedsDigit");

/** `currentPassword` is required unless the caller passes `hasPassword: false` (BR-05, OAuth-only account setting a password for the first time). */
export function changePasswordSchema(hasPassword: boolean) {
  return z
    .object({
      currentPassword: hasPassword
        ? z.string().min(1, "errors.currentPasswordRequired")
        : z.string().optional().default(""),
      newPassword: newPasswordField,
      confirmPassword: z.string(),
    })
    .refine((value) => value.newPassword === value.confirmPassword, {
      message: "errors.passwordMismatch",
      path: ["confirmPassword"],
    });
}

export type ChangePasswordInput = { currentPassword: string; newPassword: string; confirmPassword: string };

export const emailOtpSchema = z.object({
  code: z.string().length(6, "errors.otpLength").regex(/^\d+$/, "errors.otpDigitsOnly"),
});

export type EmailOtpInput = z.infer<typeof emailOtpSchema>;

/** Danger-zone confirm phrase must match the account's own email — checked client-side to unlock the button, server does the real check (Sheet 6 Khu vực I NO 5). */
export const deleteAccountSchema = (email: string) =>
  z.object({
    confirmPhrase: z.literal(email, { message: "errors.confirmPhraseMismatch" }),
  });
