// Access Token giữ trong bộ nhớ ứng dụng, KHÔNG BAO GIỜ localStorage — XSS đọc được
// localStorage. Refresh Token nằm trong cookie HttpOnly do backend set, JS không đọc được.
// Nguồn: 01-rd/system/frontend_architecture.md mục 4.
let accessToken: string | null = null;

export const tokenStore = {
  get(): string | null {
    return accessToken;
  },
  set(token: string | null): void {
    accessToken = token;
  },
};
