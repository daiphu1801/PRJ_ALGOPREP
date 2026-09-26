// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useCallback, useState } from "react";
import { changeMyPassword, changePasswordSchema } from "@/entities/user";

export function useChangePassword(hasPassword: boolean, onChanged: () => void) {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const reset = useCallback(() => {
    setCurrentPassword("");
    setNewPassword("");
    setConfirmPassword("");
    setFieldErrors({});
  }, []);

  const submit = useCallback(async () => {
    const parsed = changePasswordSchema(hasPassword).safeParse({ currentPassword, newPassword, confirmPassword });
    if (!parsed.success) {
      const errors: Record<string, string> = {};
      for (const issue of parsed.error.issues) errors[issue.path.join(".") || "form"] ??= issue.message;
      setFieldErrors(errors);
      return;
    }
    setIsSubmitting(true);
    try {
      const outcome = await changeMyPassword(parsed.data);
      if (!outcome.ok) {
        setFieldErrors({ currentPassword: outcome.message });
        return;
      }
      reset();
      onChanged();
    } finally {
      setIsSubmitting(false);
    }
  }, [hasPassword, currentPassword, newPassword, confirmPassword, reset, onChanged]);

  return { currentPassword, setCurrentPassword, newPassword, setNewPassword, confirmPassword, setConfirmPassword, fieldErrors, isSubmitting, submit, reset };
}
