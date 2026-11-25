"use client";

import { RotateCcw, Save, UserRound } from "lucide-react";
import { useT } from "@/shared/i18n";
import { useApiErrorMessage } from "@/shared/i18n/use-api-error-message";
import { toast } from "@/shared/lib/toast-store";
import { Button, Card, TextField } from "@/shared/ui";
import type { UserProfile } from "@/entities/user";
import { useEditProfile } from "../model/use-edit-profile";
import { EmailOtpDialog } from "./email-otp-dialog";

const FIELD_GRID =
  "grid gap-3 @min-[28rem]:grid-cols-2 @min-[35rem]:grid-cols-3";
const FIELD_LEGEND = "mb-3 text-sm font-semibold text-[var(--color-text)]";

export function PersonalInfoCard({
  profile,
  showTargetPosition = true,
  onProfileSaved,
}: {
  profile: UserProfile;
  showTargetPosition?: boolean;
  onProfileSaved: (next: UserProfile) => void;
}) {
  const t = useT("profile");
  const apiMessage = useApiErrorMessage();
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
  } = useEditProfile(profile, onProfileSaved, showTargetPosition);

  async function handleSave() {
    const result = await save();
    if (result.ok) toast.success(t("form.savedToast"));
    else
      toast.error(
        "error" in result ? apiMessage(result.error) : t(result.errorKey),
      );
  }

  return (
    <Card
      className="@container"
      title={
        <span className="flex items-center gap-2">
          <UserRound
            size={18}
            aria-hidden="true"
            className="text-[var(--color-primary)]"
          />
          {t("form.title")}
        </span>
      }
    >
      <form
        className="mt-5 flex flex-col gap-5"
        onSubmit={(e) => {
          e.preventDefault();
          void handleSave();
        }}
      >
        <fieldset className="min-w-0">
          <legend className={FIELD_LEGEND}>{t("form.contactGroup")}</legend>
          <div className={FIELD_GRID}>
            <TextField
              label={t("form.displayName")}
              autoComplete="name"
              className="h-11 text-sm"
              value={fields.displayName}
              onChange={(e) => update("displayName", e.target.value)}
              invalid={Boolean(fieldErrors.displayName)}
              disabled={isSaving}
              maxLength={100}
            />
            <TextField
              label={t("form.email")}
              type="email"
              autoComplete="email"
              className="h-11 text-sm"
              value={fields.email}
              onChange={(e) => update("email", e.target.value)}
              invalid={Boolean(fieldErrors.email)}
              disabled={isSaving || Boolean(emailOtpTarget)}
              maxLength={254}
            />
            <TextField
              label={t("form.phone")}
              type="tel"
              autoComplete="tel"
              className="h-11 text-sm"
              value={fields.phone}
              onChange={(e) => update("phone", e.target.value)}
              invalid={Boolean(fieldErrors.phone)}
              disabled={isSaving}
              maxLength={20}
            />
          </div>
        </fieldset>
        <fieldset className="min-w-0">
          <legend className={FIELD_LEGEND}>{t("form.workGroup")}</legend>
          <div
            className={
              showTargetPosition
                ? FIELD_GRID
                : "grid gap-3 @min-[28rem]:grid-cols-2"
            }
          >
            <TextField
              label={t("form.schoolOrCompany")}
              autoComplete="organization"
              className="h-11 text-sm"
              value={fields.schoolOrCompany}
              onChange={(e) => update("schoolOrCompany", e.target.value)}
              invalid={Boolean(fieldErrors.schoolOrCompany)}
              disabled={isSaving}
              maxLength={150}
            />
            <TextField
              label={t("form.currentPosition")}
              autoComplete="organization-title"
              className="h-11 text-sm"
              value={fields.currentPosition}
              onChange={(e) => update("currentPosition", e.target.value)}
              invalid={Boolean(fieldErrors.currentPosition)}
              disabled={isSaving}
              maxLength={100}
            />
            {showTargetPosition ? (
              <TextField
                label={t("form.targetPosition")}
                className="h-11 text-sm"
                value={fields.targetPosition}
                onChange={(e) => update("targetPosition", e.target.value)}
                invalid={Boolean(fieldErrors.targetPosition)}
                disabled={isSaving}
                maxLength={100}
              />
            ) : null}
          </div>
        </fieldset>

        <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2 border-t border-[var(--color-border)] pt-4">
          <div
            role="status"
            className="text-xs font-medium text-[var(--color-text-muted)]"
          >
            {dirty ? t("form.unsaved") : null}
          </div>
          <div className="ml-auto flex flex-wrap gap-2">
            <Button
              type="button"
              variant="ghost"
              className="h-11 gap-2 px-3"
              onClick={revert}
              disabled={!dirty || isSaving}
            >
              <RotateCcw size={16} aria-hidden="true" />
              {t("form.revert")}
            </Button>
            <Button
              type="submit"
              className="h-11 gap-2 px-3"
              disabled={!dirty || isSaving}
              aria-busy={isSaving || undefined}
            >
              <Save size={16} aria-hidden="true" />
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
