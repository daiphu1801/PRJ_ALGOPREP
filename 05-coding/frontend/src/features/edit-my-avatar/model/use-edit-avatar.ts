// State of the profile-picture editor (DD USR0502 BR-SC-07): pick, downscale, preview, save,
// cancel, remove. Independent from the profile form; only the leave guard is shared with it.
"use client";

import { useCallback, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useT } from "@/shared/i18n";
import { useApiErrorMessage } from "@/shared/i18n/use-api-error-message";
import { downscaleToSquare } from "@/shared/lib/downscale-image";
import { checkImageFile, IMAGE_TYPES } from "@/shared/lib/image-file";
import { useObjectUrl } from "@/shared/lib/use-object-url";
import { toast } from "@/shared/lib/toast-store";
import { removeMyAvatar, uploadMyAvatar } from "@/entities/user";

export const ACCEPTED_AVATAR_TYPES = IMAGE_TYPES;
export const MAX_RAW_AVATAR_BYTES = 10 * 1024 * 1024;

type Pending = { blob: Blob; filename: string };

export function useEditAvatar() {
  const t = useT("profile");
  const apiMessage = useApiErrorMessage();
  const queryClient = useQueryClient();
  const [pending, setPending] = useState<Pending | null>(null);
  const [busy, setBusy] = useState(false);
  const pendingRef = useRef<Pending | null>(null);
  const previewUrl = useObjectUrl(pending?.blob);

  const replacePending = useCallback((next: Pending | null) => {
    pendingRef.current = next;
    setPending(next);
  }, []);

  const choose = useCallback(
    async (file: File) => {
      const problem = checkImageFile(file, MAX_RAW_AVATAR_BYTES);
      if (problem) {
        toast.error(
          t(
            problem === "type"
              ? "avatar.fileTypeError"
              : "avatar.fileTooBigError",
          ),
        );
        return;
      }
      try {
        replacePending(await downscaleToSquare(file, 256));
      } catch {
        toast.error(t("avatar.decodeError"));
      }
    },
    [replacePending, t],
  );
  /** Returns true when the picture was stored; the preview stays on failure so the user can retry. */
  const save = useCallback(async (): Promise<boolean> => {
    const current = pendingRef.current;
    if (!current || busy) return false;
    setBusy(true);
    try {
      await uploadMyAvatar(current.blob, current.filename);
      await queryClient.invalidateQueries({ queryKey: ["user", "my-profile"] });
      replacePending(null);
      toast.success(t("avatar.uploaded"));
      return true;
    } catch (error) {
      toast.error(apiMessage(error));
      return false;
    } finally {
      setBusy(false);
    }
  }, [apiMessage, busy, queryClient, replacePending, t]);

  const cancel = useCallback(() => replacePending(null), [replacePending]);

  const remove = useCallback(async (): Promise<boolean> => {
    setBusy(true);
    try {
      await removeMyAvatar();
      await queryClient.invalidateQueries({ queryKey: ["user", "my-profile"] });
      replacePending(null);
      toast.success(t("avatar.removed"));
      return true;
    } catch (error) {
      toast.error(apiMessage(error));
      return false;
    } finally {
      setBusy(false);
    }
  }, [apiMessage, queryClient, replacePending, t]);

  return { pending, previewUrl, busy, choose, save, cancel, remove };
}
