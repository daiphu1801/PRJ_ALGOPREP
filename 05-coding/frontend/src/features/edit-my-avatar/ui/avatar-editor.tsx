"use client";

import { useState } from "react";
import { Camera, Check, Trash2, X } from "lucide-react";
import { useT } from "@/shared/i18n";
import { useUnsavedChangesGuard } from "@/shared/lib/use-unsaved-changes-guard";
import {
  Avatar,
  Button,
  ConfirmDialog,
  FilePickerButton,
  UnsavedChangesDialog,
} from "@/shared/ui";
import { UserAvatar } from "@/entities/user";
import { ACCEPTED_AVATAR_TYPES, useEditAvatar } from "../model/use-edit-avatar";

/**
 * Avatar block of the identity card (BD USR0502 Khu vực A NO 1, 4-8). A chosen picture waits as a
 * preview until "Lưu ảnh"; a pending preview counts as an unsaved change for leaving the page
 * (EVT-15). The profile form has no leave guard yet (DD P3), so this one covers only the picture.
 */
export function AvatarEditor({
  userId,
  name,
  email,
  avatarVersion,
}: {
  userId: string;
  name: string;
  email?: string;
  avatarVersion?: number | null;
}) {
  const t = useT("profile");
  const { pending, previewUrl, busy, choose, save, cancel, remove } =
    useEditAvatar();
  const [confirmRemove, setConfirmRemove] = useState(false);
  const guard = useUnsavedChangesGuard(pending !== null);

  return (
    <div className="flex w-full flex-wrap items-center justify-between gap-x-6 gap-y-4">
      <div className="flex min-w-0 flex-[1_1_15rem] items-center gap-4">
        {pending ? (
          <Avatar
            src={previewUrl}
            name={t("avatar.previewAlt")}
            size="lg"
            className="ring-2 ring-[var(--color-primary)] ring-offset-2 ring-offset-[var(--color-background)]"
          />
        ) : (
          <UserAvatar
            userId={userId}
            name={name}
            avatarVersion={avatarVersion}
            size="lg"
          />
        )}
        <div className="min-w-0 wrap-anywhere">
          <p className="text-xl font-bold tracking-tight">{name}</p>
          {email ? (
            <p className="text-sm text-[var(--color-text-subtle)]">{email}</p>
          ) : null}
          <div role="status">
            {pending ? (
              <span className="mt-1 inline-block text-xs font-medium text-[var(--color-text-subtle)]">
                {t("avatar.previewLabel")}
              </span>
            ) : null}
          </div>
        </div>
      </div>
      <div className="flex flex-wrap items-center gap-2">
        {pending ? (
          <>
            <Button
              type="button"
              variant="cta"
              className="h-11 gap-2 px-3"
              onClick={() => void save()}
              disabled={busy}
            >
              <Check size={16} aria-hidden="true" />
              {t("avatar.save")}
            </Button>
            <Button
              type="button"
              variant="ghost"
              className="h-11 gap-2 px-3"
              onClick={cancel}
              disabled={busy}
            >
              <X size={16} aria-hidden="true" />
              {t("avatar.cancel")}
            </Button>
          </>
        ) : null}
        <FilePickerButton
          type="button"
          variant="ghost"
          className="h-11 gap-2 border border-[var(--color-border)] px-3"
          accept={ACCEPTED_AVATAR_TYPES.join(",")}
          disabled={busy}
          inputTestId="avatar-file-input"
          onFiles={(files) => files[0] && void choose(files[0])}
        >
          <Camera size={16} aria-hidden="true" />
          {t("avatar.change")}
        </FilePickerButton>
        {avatarVersion != null && !pending ? (
          <Button
            type="button"
            variant="ghost"
            className="h-11 gap-2 px-3 text-[var(--color-danger)]"
            disabled={busy}
            onClick={() => setConfirmRemove(true)}
          >
            <Trash2 size={16} aria-hidden="true" />
            {t("avatar.remove")}
          </Button>
        ) : null}
      </div>

      <ConfirmDialog
        open={confirmRemove}
        onClose={() => setConfirmRemove(false)}
        title={t("avatar.removeTitle")}
        confirmLabel={t("avatar.removeConfirm")}
        cancelLabel={t("avatar.cancel")}
        destructive
        pending={busy}
        onConfirm={async () => {
          await remove();
          setConfirmRemove(false);
        }}
      >
        {t("avatar.removeBody")}
      </ConfirmDialog>

      <UnsavedChangesDialog
        open={guard.pending}
        title={t("avatar.leaveTitle")}
        saveAndLeaveLabel={t("avatar.saveAndLeave")}
        leaveLabel={t("avatar.leave")}
        stayLabel={t("avatar.stay")}
        pending={busy}
        onSaveAndLeave={async () => {
          if (await save()) guard.leave();
          else guard.stay();
        }}
        onLeave={guard.leave}
        onStay={guard.stay}
      >
        {t("avatar.leaveBody")}
      </UnsavedChangesDialog>
    </div>
  );
}
