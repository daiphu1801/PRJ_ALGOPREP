// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useCallback, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { changeMyPassword, changePasswordSchema } from "@/entities/user";

/**
 * Outcome of `submit`: the i18n key (profile namespace) of the first error, or, when the API
 * itself failed, the error for `useApiErrorMessage`, so the dialog can toast either.
 */
export type ChangePasswordResult =
  | { ok: true; noticeEmailFailed?: boolean }
  | { ok: false; errorKey: string }
  | { ok: false; error: unknown };

export function useChangePassword(hasPassword: boolean, onChanged: () => void) {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const queryClient = useQueryClient();

  const reset = useCallback(() => {
    setCurrentPassword("");
    setNewPassword("");
    setConfirmPassword("");
    setFieldErrors({});
  }, []);

  const submit = useCallback(async (): Promise<ChangePasswordResult> => {
    const parsed = changePasswordSchema(hasPassword).safeParse({
      currentPassword,
      newPassword,
      confirmPassword,
    });
    if (!parsed.success) {
      const errors: Record<string, string> = {};
      for (const issue of parsed.error.issues)
        errors[issue.path.join(".") || "form"] ??= issue.message;
      setFieldErrors(errors);
      return {
        ok: false,
        errorKey: Object.values(errors)[0] ?? "passwordDialog.invalid",
      };
    }
    setIsSubmitting(true);
    try {
      const outcome = await changeMyPassword(parsed.data);
      if (!outcome.ok) {
        setFieldErrors({ currentPassword: outcome.message });
        return { ok: false, errorKey: outcome.message };
      }
      reset();
      // D22: refresh "changed at" / hasPassword on the Settings Security card.
      void queryClient.invalidateQueries({ queryKey: ["user", "my-profile"] });
      onChanged();
      return outcome.noticeEmailFailed
        ? { ok: true, noticeEmailFailed: true }
        : { ok: true };
    } catch (error) {
      return { ok: false, error };
    } finally {
      setIsSubmitting(false);
    }
  }, [
    hasPassword,
    currentPassword,
    newPassword,
    confirmPassword,
    reset,
    onChanged,
    queryClient,
  ]);

  return {
    currentPassword,
    setCurrentPassword,
    newPassword,
    setNewPassword,
    confirmPassword,
    setConfirmPassword,
    fieldErrors,
    isSubmitting,
    submit,
    reset,
  };
}
