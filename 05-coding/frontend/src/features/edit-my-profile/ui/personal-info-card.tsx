// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
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
    emailOtpTarget,
    update,
    revert,
    save,
    closeEmailOtp,
    onEmailConfirmed,
  } = useEditProfile(profile, onProfileSaved);

  async function handleSave() {
    const result = await save();
    if (result.ok) toast.success(t("form.savedToast"));
    else toast.error(t(result.errorKey));
  }

  return (
    <Card title={t("form.title")}>
      <form
        className="grid grid-cols-1 gap-4 sm:grid-cols-2"
        onSubmit={(e) => {
          e.preventDefault();
          void handleSave();
        }}
      >
        <TextField
          label={t("form.displayName")}
          value={fields.displayName}
          onChange={(e) => update("displayName", e.target.value)}
          invalid={Boolean(fieldErrors.displayName)}
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
          invalid={Boolean(fieldErrors.schoolOrCompany)}
          disabled={isSaving}
          maxLength={150}
        />
        <TextField
          label={t("form.currentPosition")}
          value={fields.currentPosition}
          onChange={(e) => update("currentPosition", e.target.value)}
          invalid={Boolean(fieldErrors.currentPosition)}
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
          invalid={Boolean(fieldErrors.targetPosition)}
          disabled={isSaving}
          maxLength={100}
        />

        <div className="col-span-full flex flex-wrap items-center gap-3 border-t border-[var(--color-border)] pt-4">
          <div className="ml-auto flex gap-2">
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={revert}
              disabled={!dirty || isSaving}
            >
              {t("form.revert")}
            </Button>
            <Button
              type="submit"
              size="sm"
              disabled={!dirty || isSaving}
              aria-busy={isSaving || undefined}
            >
              {t("form.save")}
            </Button>
          </div>
        </div>
      </form>

      {emailOtpTarget ? (
        <EmailOtpDialog
          newEmail={emailOtpTarget}
          onCancel={closeEmailOtp}
          onConfirmed={onEmailConfirmed}
        />
      ) : null}
    </Card>
  );
}
