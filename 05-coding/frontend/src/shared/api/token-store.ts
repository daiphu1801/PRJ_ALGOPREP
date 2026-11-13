// The Access Token is kept in application memory, NEVER localStorage — XSS can read
// localStorage. The Refresh Token lives in an HttpOnly cookie set by the backend, which JS
// cannot read. Source: 01-rd/system/SYS0102_frontend_architecture.md section 4.
let accessToken: string | null = null;
let sessionGeneration = 0;
const listeners = new Set<() => void>();

function identity(token: string | null): string | null {
  if (!token) return null;
  try {
    const raw = token.split(".")[1];
    if (!raw) return token;
    const payload = raw.replaceAll("-", "+").replaceAll("_", "/");
    const decoded: unknown = JSON.parse(atob(payload));
    if (
      decoded &&
      typeof decoded === "object" &&
      "sub" in decoded &&
      typeof decoded.sub === "string"
    ) {
      return decoded.sub;
    }
  } catch {
    // Malformed tokens must not share a session with another token.
  }
  return token;
}

export const tokenStore = {
  get(): string | null {
    return accessToken;
  },
  set(token: string | null): void {
    const changed = identity(accessToken) !== identity(token);
    accessToken = token;
    if (changed) {
      sessionGeneration += 1;
      for (const listener of listeners) listener();
    }
  },
  /** A logout and subsequent login start a new session, even for the same subject. */
  generation: (): number => sessionGeneration,
  subscribe(listener: () => void): () => void {
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  },
};
