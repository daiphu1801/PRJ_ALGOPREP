"use client";

import { useState } from "react";
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
  avatarVersion,
}: {
  userId: string;
  name: string;
  avatarVersion?: number | null;
}) {
  const t = useT("profile");
  const { pending, previewUrl, busy, choose, save, cancel, remove } =
    useEditAvatar();
  const [confirmRemove, setConfirmRemove] = useState(false);
  const guard = useUnsavedChangesGuard(pending !== null);

  return (
    <div className="flex flex-wrap items-center gap-4">
      {pending ? (
        <div className="flex flex-col items-center gap-1">
          <Avatar src={previewUrl} name={t("avatar.previewAlt")} size="lg" />
          <span className="text-xs text-[var(--color-text-subtle)]">
            {t("avatar.previewLabel")}
          </span>
        </div>
      ) : (
        <UserAvatar
          userId={userId}
          name={name}
          avatarVersion={avatarVersion}
          size="lg"
        />
      )}
      <div className="flex flex-wrap items-center gap-2">
        {pending ? (
          <>
            <Button size="sm" onClick={() => void save()} disabled={busy}>
              {t("avatar.save")}
            </Button>
            <Button size="sm" variant="ghost" onClick={cancel} disabled={busy}>
              {t("avatar.cancel")}
            </Button>
          </>
        ) : null}
        <FilePickerButton
          size="sm"
          variant="ghost"
          accept={ACCEPTED_AVATAR_TYPES.join(",")}
          disabled={busy}
          inputTestId="avatar-file-input"
          onFiles={(files) => files[0] && void choose(files[0])}
        >
          {t("avatar.change")}
        </FilePickerButton>
        {avatarVersion != null || pending ? (
          <Button
            size="sm"
            variant="ghost"
            disabled={busy}
            onClick={() => setConfirmRemove(true)}
          >
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
