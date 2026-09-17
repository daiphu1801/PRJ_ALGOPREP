// The Access Token is kept in application memory, NEVER localStorage — XSS can read
// localStorage. The Refresh Token lives in an HttpOnly cookie set by the backend, which JS
// cannot read. Source: 01-rd/system/frontend_architecture.md section 4.
let accessToken: string | null = null;

export const tokenStore = {
  get(): string | null {
    return accessToken;
  },
  set(token: string | null): void {
    accessToken = token;
  },
};
