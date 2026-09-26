// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useT } from "@/shared/i18n";
import { Button, Card, SelectField, TextField } from "@/shared/ui";
import type { UserProfile } from "@/entities/user";
import { useEditProfile } from "../model/use-edit-profile";
import { EmailOtpDialog } from "./email-otp-dialog";

export function PersonalInfoCard({
  profile,
  onProfileSaved,
}: {
  profile: UserProfile;
  onProfileSaved: (next: UserProfile) => void;
}) {
  const t = useT("profile");
  const {
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
  } = useEditProfile(profile, onProfileSaved);

  const statusText = justSaved
    ? t("form.statusSaved")
    : dirty
      ? t("form.statusDirty")
      : t("form.statusIdle");

  return (
    <Card title={t("form.title")}>
      <form
        className="grid grid-cols-1 gap-4 sm:grid-cols-2"
        onSubmit={(e) => {
          e.preventDefault();
          void save();
        }}
      >
        <TextField
          label={t("form.displayName")}
          value={fields.displayName}
          onChange={(e) => update("displayName", e.target.value)}
          error={fieldErrors.displayName ? t(fieldErrors.displayName) : undefined}
          disabled={isSaving}
          maxLength={100}
        />
        <TextField
          label={t("form.email")}
          type="email"
          value={fields.email}
          onChange={(e) => update("email", e.target.value)}
          disabled={isSaving || Boolean(emailOtpTarget)}
          maxLength={254}
        />
        <TextField
          label={t("form.schoolOrCompany")}
          value={fields.schoolOrCompany}
          onChange={(e) => update("schoolOrCompany", e.target.value)}
          error={fieldErrors.schoolOrCompany ? t(fieldErrors.schoolOrCompany) : undefined}
          disabled={isSaving}
          maxLength={150}
        />
        <TextField
          label={t("form.currentPosition")}
          value={fields.currentPosition}
          onChange={(e) => update("currentPosition", e.target.value)}
          error={fieldErrors.currentPosition ? t(fieldErrors.currentPosition) : undefined}
          disabled={isSaving}
          maxLength={100}
        />
        <SelectField
          label={t("form.defaultLanguage")}
          value={fields.defaultLanguage}
          onChange={(e) => update("defaultLanguage", e.target.value)}
          disabled={isSaving}
          options={[
            { value: "PYTHON", label: "Python 3" },
            { value: "JAVA", label: "Java 21" },
            { value: "CPP", label: "C++ 17" },
          ]}
        />
        <TextField
          label={t("form.targetPosition")}
          value={fields.targetPosition}
          onChange={(e) => update("targetPosition", e.target.value)}
          error={fieldErrors.targetPosition ? t(fieldErrors.targetPosition) : undefined}
          disabled={isSaving}
          maxLength={100}
        />

        <div className="col-span-full flex flex-wrap items-center gap-3 border-t border-[var(--color-border)] pt-4">
          <p className="text-xs text-[var(--color-text-muted)]">{statusText}</p>
          <div className="ml-auto flex gap-2">
            <Button type="button" variant="ghost" size="sm" onClick={revert} disabled={!dirty || isSaving}>
              {t("form.revert")}
            </Button>
            <Button type="submit" size="sm" disabled={!dirty || isSaving} aria-busy={isSaving || undefined}>
              {justSaved ? t("form.saved") : t("form.save")}
            </Button>
          </div>
        </div>
      </form>

      {emailOtpTarget ? (
        <EmailOtpDialog newEmail={emailOtpTarget} onCancel={closeEmailOtp} onConfirmed={onEmailConfirmed} />
      ) : null}
    </Card>
  );
}
