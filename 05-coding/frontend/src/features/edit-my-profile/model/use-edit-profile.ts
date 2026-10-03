// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useCallback, useMemo, useState } from "react";
import type { ZodError } from "zod";
import {
  profileFormSchema,
  requestEmailChange,
  updateMyProfile,
  type ProfileFormInput,
  type SubmissionLanguage,
  type UserProfile,
} from "@/entities/user";

type FormValues = {
  displayName: string;
  email: string;
  schoolOrCompany: string;
  currentPosition: string;
  defaultLanguage: SubmissionLanguage;
  targetPosition: string;
};

function toFormValues(profile: UserProfile): FormValues {
  return {
    displayName: profile.displayName,
    email: profile.email,
    schoolOrCompany: profile.schoolOrCompany ?? "",
    currentPosition: profile.currentPosition ?? "",
    defaultLanguage: profile.defaultLanguage,
    targetPosition: profile.targetPosition ?? "",
  };
}

function zodErrorsToFieldErrors(error: ZodError): Record<string, string> {
  const result: Record<string, string> = {};
  for (const issue of error.issues) {
    const key = issue.path.join(".") || "form";
    result[key] ??= issue.message;
  }
  return result;
}

/** Outcome of `save`: the i18n key (profile namespace) of the first error, so the card can toast it. */
export type SaveProfileResult = { ok: true } | { ok: false; errorKey: string };

/**
 * Owns Khu vực B (personal-info form) of USR0502_profile. `email` is edited here but is NEVER part
 * of `UpdateMyProfileInput` (03-dd/api/identity.md endpoint 10 note) — changing it fires
 * `RequestMyEmailChange` and opens the OTP dialog, independent of whether the other 5 fields saved.
 */
export function useEditProfile(profile: UserProfile, onProfileSaved: (next: UserProfile) => void) {
  const [saved, setSaved] = useState(() => toFormValues(profile));
  const [fields, setFields] = useState(saved);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [isSaving, setIsSaving] = useState(false);
  const [justSaved, setJustSaved] = useState(false);
  const [emailOtpTarget, setEmailOtpTarget] = useState<string | null>(null);

  const dirty = useMemo(
    () => (Object.keys(saved) as (keyof FormValues)[]).some((key) => saved[key] !== fields[key]),
    [saved, fields],
  );

  const update = useCallback((name: keyof FormValues, value: string) => {
    setFields((prev) => ({ ...prev, [name]: value }));
    setJustSaved(false);
    setFieldErrors((prev) => {
      if (!(name in prev)) return prev;
      const next = { ...prev };
      delete next[name];
      return next;
    });
  }, []);

  const revert = useCallback(() => {
    setFields(saved);
    setFieldErrors({});
    setJustSaved(false);
  }, [saved]);

  const save = useCallback(async (): Promise<SaveProfileResult> => {
    const input: ProfileFormInput = {
      displayName: fields.displayName,
      schoolOrCompany: fields.schoolOrCompany,
      currentPosition: fields.currentPosition,
      defaultLanguage: fields.defaultLanguage,
      targetPosition: fields.targetPosition,
    };
    const parsed = profileFormSchema.safeParse(input);
    if (!parsed.success) {
      const errors = zodErrorsToFieldErrors(parsed.error);
      setFieldErrors(errors);
      return { ok: false, errorKey: Object.values(errors)[0] ?? "form.invalid" };
    }

    setIsSaving(true);
    try {
      const emailChanged = fields.email !== saved.email;
      const [nextProfile] = await Promise.all([
        updateMyProfile(parsed.data),
        emailChanged ? requestEmailChange(fields.email) : Promise.resolve(null),
      ]);
      onProfileSaved(nextProfile);
      setSaved((prev) => ({ ...toFormValues(nextProfile), email: prev.email }));
      setJustSaved(true);
      if (emailChanged) setEmailOtpTarget(fields.email);
      return { ok: true };
    } finally {
      setIsSaving(false);
    }
  }, [fields, saved.email, onProfileSaved]);

  const closeEmailOtp = useCallback(() => setEmailOtpTarget(null), []);

  const onEmailConfirmed = useCallback(() => {
    setSaved((prev) => ({ ...prev, email: fields.email }));
    setEmailOtpTarget(null);
  }, [fields.email]);

  return {
    fields,
    fieldErrors,
    dirty,
    isSaving,
    justSaved,
    emailOtpTarget,
    update,
    revert,
    save,
    closeEmailOtp,
    onEmailConfirmed,
  };
}
