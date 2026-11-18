// Real calls for `POST /identity/me/email-change/request` and `/confirm` (03-dd/api/F1_identity.md
// #11 and #12, section 0.2.2). A wrong or expired code becomes an outcome the dialog marks on its
// field; every other failure (taken address, rate limit, outage) is rethrown as `ApiError` so the
// caller shows it through `apiErrors`.
import { api, ApiError } from "@/shared/api";

const BASE = "/identity/me/email-change";

export async function realRequestEmailChange(
  newEmail: string,
): Promise<{ ok: true }> {
  await api.post(`${BASE}/request`, { newEmail });
  return { ok: true };
}

export async function realConfirmEmailChange(
  code: string,
): Promise<{ ok: true } | { ok: false; message: string }> {
  try {
    await api.post(`${BASE}/confirm`, { code });
    return { ok: true };
  } catch (error) {
    if (error instanceof ApiError && error.code === "IDT-416") {
      return { ok: false, message: "errors.otpInvalid" };
    }
    throw error;
  }
}
