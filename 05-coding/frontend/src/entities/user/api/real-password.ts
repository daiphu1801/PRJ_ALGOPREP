// Real call for `POST /identity/me/password` (03-dd/api/F1_identity.md #13, BR-06). A wrong current
// password becomes an outcome the dialog marks on its field; everything else is rethrown as
// `ApiError` for the caller to show through `apiErrors`.
import { api, ApiError } from "@/shared/api";
import type { ChangePasswordInput } from "../model/schema";

export async function realChangeMyPassword(
  input: ChangePasswordInput,
): Promise<{ ok: true } | { ok: false; message: string }> {
  try {
    await api.post("/identity/me/password", {
      currentPassword: input.currentPassword,
      newPassword: input.newPassword,
    });
    return { ok: true };
  } catch (error) {
    if (error instanceof ApiError && error.code === "IDT-115") {
      return { ok: false, message: "errors.currentPasswordInvalid" };
    }
    throw error;
  }
}
