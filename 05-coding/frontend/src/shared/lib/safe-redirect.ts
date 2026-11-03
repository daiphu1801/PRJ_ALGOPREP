/**
 * True only for a path inside this app. A value that starts with "//" or "/\" is read by the
 * browser as another site, so "starts with /" alone would let `?redirect=//evil.example` send a
 * freshly signed-in user away.
 */
export function isSafeRedirect(path: string | null | undefined): path is string {
  return (
    typeof path === "string" &&
    path.startsWith("/") &&
    !path.startsWith("//") &&
    !path.startsWith("/\\")
  );
}
