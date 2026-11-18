// Real calls for the profile picture (03-dd/api/F1_identity.md section 0.9, #74 to #77).
import { api } from "@/shared/api";

const ME_AVATAR = "/identity/me/avatar";

/** `PUT /me/avatar` (#74): multipart, exactly one part named `file`. */
export async function realUploadMyAvatar(
  blob: Blob,
  filename: string,
): Promise<{ avatarVersion: number }> {
  const form = new FormData();
  form.append("file", blob, filename);
  return api.put<{ avatarVersion: number }>(ME_AVATAR, form);
}

/** `DELETE /me/avatar` (#75); `204`, idempotent. */
export async function realRemoveMyAvatar(): Promise<void> {
  await api.delete(ME_AVATAR);
}

/** `DELETE /admin/users/{userId}/avatar` (#77). */
export async function realRemoveUserAvatar(userId: string): Promise<void> {
  await api.delete(
    `/identity/admin/users/${encodeURIComponent(userId)}/avatar`,
  );
}

/** `GET /users/{userId}/avatar` (#76) as a Blob; `v` only separates the cache entry per version. */
export function realGetUserAvatar(
  userId: string,
  avatarVersion: number,
): Promise<Blob> {
  return api.getBlob(`/identity/users/${encodeURIComponent(userId)}/avatar`, {
    v: avatarVersion,
  });
}
