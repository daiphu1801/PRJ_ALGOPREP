// Real call for `POST /identity/me/account/delete` (03-dd/api/F1_identity.md #18, BR-08). A typed
// e-mail that does not match becomes an outcome the dialog marks on its field; everything else is
// rethrown as `ApiError`. On success the server has revoked every session, so the in-memory access
// token is dropped here too and the caller only has to leave the page.
import { api, ApiError, tokenStore } from "@/shared/api";

export async function realDeleteMyAccount(
  emailConfirmation: string,
): Promise<{ ok: true } | { ok: false; message: string }> {
  try {
    await api.post("/identity/me/account/delete", { emailConfirmation });
    tokenStore.set(null);
    return { ok: true };
  } catch (error) {
    if (error instanceof ApiError && error.code === "IDT-201") {
      return { ok: false, message: "errors.confirmPhraseMismatch" };
    }
    throw error;
  }
}
