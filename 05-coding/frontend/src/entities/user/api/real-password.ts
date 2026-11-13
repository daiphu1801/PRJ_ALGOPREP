// Real call for `POST /identity/me/password` (03-dd/api/F1_identity.md #13, BR-06). A wrong current
// password becomes an outcome the dialog marks on its field; everything else is rethrown as
// `ApiError` for the caller to show through `apiErrors`. A 200 may carry `noticeEmailFailed`: the
// password changed but the first-password notice e-mail could not be sent (best effort, AC-15).
import { api, ApiError } from "@/shared/api";
import type { ChangePasswordInput } from "../model/schema";

export async function realChangeMyPassword(
  input: ChangePasswordInput,
): Promise<
  { ok: true; noticeEmailFailed?: boolean } | { ok: false; message: string }
> {
  try {
    const res = await api.post<{ noticeEmailFailed?: boolean } | undefined>(
      "/identity/me/password",
      {
        currentPassword: input.currentPassword,
        newPassword: input.newPassword,
      },
    );
    // The password is already changed; the flag only says the notice e-mail could not be sent.
    return res?.noticeEmailFailed
      ? { ok: true, noticeEmailFailed: true }
      : { ok: true };
  } catch (error) {
    if (error instanceof ApiError && error.code === "IDT-115") {
      return { ok: false, message: "errors.currentPasswordInvalid" };
    }
    throw error;
  }
}
