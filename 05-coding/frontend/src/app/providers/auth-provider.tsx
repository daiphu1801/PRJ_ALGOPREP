"use client";

import type { ReactNode } from "react";

/**
 * TODO(session-bootstrap): restore the session (call /auth/refresh using the HttpOnly cookie)
 * on app startup, so the access token in tokenStore doesn't get lost after a page reload. Not
 * built yet because 03-dd/api/identity.md does not exist — if a feature needs a restored
 * session, this must be done first (see nextjs-fsd-expert Layer 6).
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  return <>{children}</>;
}
