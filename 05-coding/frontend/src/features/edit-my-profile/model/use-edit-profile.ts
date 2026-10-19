// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useCallback, useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import type { ZodError } from "zod";
import {
  emailChangeSchema,
  profileFormSchema,
  requestEmailChange,
  updateMyProfile,
  type ProfileFormInput,
  type UserProfile,
} from "@/entities/user";

type FormValues = {
  displayName: string;
  email: string;
  schoolOrCompany: string;
  currentPosition: string;
  targetPosition: string;
};

function toFormValues(profile: UserProfile): FormValues {
  return {
    displayName: profile.displayName,
    email: profile.email,
    schoolOrCompany: profile.schoolOrCompany ?? "",
    currentPosition: profile.currentPosition ?? "",
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
export function useEditProfile(
  profile: UserProfile,
  onProfileSaved: (next: UserProfile) => void,
) {
  const [saved, setSaved] = useState(() => toFormValues(profile));
  const [fields, setFields] = useState(saved);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [isSaving, setIsSaving] = useState(false);
  const [emailOtpTarget, setEmailOtpTarget] = useState<string | null>(null);

  const dirty = useMemo(
    () =>
      (Object.keys(saved) as (keyof FormValues)[]).some(
        (key) => saved[key] !== fields[key],
      ),
    [saved, fields],
  );

  const update = useCallback((name: keyof FormValues, value: string) => {
    setFields((prev) => ({ ...prev, [name]: value }));
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
  }, [saved]);

  const save = useCallback(async (): Promise<SaveProfileResult> => {
    const input: ProfileFormInput = {
      displayName: fields.displayName,
      schoolOrCompany: fields.schoolOrCompany,
      currentPosition: fields.currentPosition,
      targetPosition: fields.targetPosition,
    };
    const parsed = profileFormSchema.safeParse(input);
    if (!parsed.success) {
      const errors = zodErrorsToFieldErrors(parsed.error);
      setFieldErrors(errors);
      return {
        ok: false,
        errorKey: Object.values(errors)[0] ?? "form.invalid",
      };
    }

    const emailChanged = fields.email.trim() !== saved.email;
    let nextEmail = saved.email;
    if (emailChanged) {
      const email = emailChangeSchema.safeParse(fields.email);
      if (!email.success) {
        const errorKey = email.error.issues[0]?.message ?? "form.invalid";
        setFieldErrors({ email: errorKey });
        return { ok: false, errorKey };
      }
      nextEmail = email.data;
    }

    setIsSaving(true);
    try {
      // D19: allSettled, so one failing path never hides the other one's success (email change is
      // independent of the profile fields per BD Sheet 7.1).
      const [profileRes, emailRes] = await Promise.allSettled([
        updateMyProfile(parsed.data),
        emailChanged ? requestEmailChange(nextEmail) : Promise.resolve(null),
      ]);
      if (profileRes.status === "fulfilled") {
        onProfileSaved(profileRes.value);
        setSaved((prev) => ({
          ...toFormValues(profileRes.value),
          email: prev.email,
        }));
      }
      if (emailRes.status === "fulfilled" && emailChanged) {
        setEmailOtpTarget(nextEmail);
      }
      if (profileRes.status === "rejected")
        return { ok: false, errorKey: "form.saveFailed" };
      if (emailRes.status === "rejected")
        return { ok: false, errorKey: "form.emailRequestFailed" };
      return { ok: true };
    } finally {
      setIsSaving(false);
    }
  }, [fields, saved.email, onProfileSaved]);

  const closeEmailOtp = useCallback(() => setEmailOtpTarget(null), []);

  const queryClient = useQueryClient();
  const onEmailConfirmed = useCallback(() => {
    setSaved((prev) => ({ ...prev, email: emailOtpTarget ?? prev.email }));
    setFields((prev) => ({ ...prev, email: emailOtpTarget ?? prev.email }));
    setEmailOtpTarget(null);
    // D21: the identity card and Settings read the email from the profile cache.
    void queryClient.invalidateQueries({ queryKey: ["user", "my-profile"] });
  }, [emailOtpTarget, queryClient]);

  return {
    fields,
    fieldErrors,
    dirty,
    isSaving,
    emailOtpTarget,
    update,
    revert,
    save,
    closeEmailOtp,
    onEmailConfirmed,
  };
}
