import { tokenStore } from "./token-store";

/** Cache partition only: the backend still validates the token and enforces every permission. */
export function currentUserScope(): string {
  const token = tokenStore.get();
  if (!token) return "anonymous";
  try {
    const payload = token.split(".")[1];
    if (!payload) return "anonymous";
    const normalized = payload.replaceAll("-", "+").replaceAll("_", "/");
    const decoded: unknown = JSON.parse(atob(normalized));
    if (
      decoded &&
      typeof decoded === "object" &&
      "sub" in decoded &&
      typeof decoded.sub === "string"
    ) {
      return decoded.sub;
    }
  } catch {
    // An invalid token may fail authentication but must never reuse another subject's cache.
  }
  return "anonymous";
}
