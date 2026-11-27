export type BaseCategory = "STUDENT" | "INSTRUCTOR" | "ADMIN";

const CATEGORIES: readonly string[] = ["STUDENT", "INSTRUCTOR", "ADMIN"];

/**
 * Reads the `base_category` claim the backend puts in the access token (token claims are `sub`,
 * `base_category`, `role_code`, `iat`, `exp`).
 *
 * This only DECODES the payload, it does not verify the signature, so the result is good for
 * choosing which page to show and nothing else. The backend re-checks every request.
 */
export function readBaseCategory(token: string | null): BaseCategory | null {
  const payload = token?.split(".")[1];
  if (!payload) return null;
  try {
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const claims: unknown = JSON.parse(atob(base64));
    const value =
      typeof claims === "object" && claims !== null
        ? (claims as Record<string, unknown>).base_category
        : undefined;
    return typeof value === "string" && CATEGORIES.includes(value)
      ? (value as BaseCategory)
      : null;
  } catch {
    return null;
  }
}
