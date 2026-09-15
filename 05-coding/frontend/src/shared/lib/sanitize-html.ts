/**
 * The frontend's SINGLE HTML sanitization point (problem statement Markdown, AI responses) —
 * everywhere else imports from here, never dompurify directly (blocked via ESLint
 * no-restricted-imports).
 *
 * NOT IMPLEMENTED YET, AND DELIBERATELY THROWS. A sanitizer library has not been chosen
 * (01-rd/system/frontend_architecture.md section 1 — "the three libraries must be decided by
 * actual need"). A fake sanitizer (e.g. a regex that strips script tags) is more dangerous than
 * none at all: it creates a false sense of safety while XSS still gets through via dozens of
 * other vectors (onerror, javascript:, SVG, unclosed tags). Throwing immediately means anyone
 * who accidentally uses it finds out at dev time, not after it has shipped to production.
 *
 * When actually implemented: server-side sanitization is layer one (Jsoup, overview.md section
 * 1.I), this function is the second line of defense on the client.
 */
export function sanitizeHtml(_rawHtml: string): string {
  throw new Error(
    "sanitizeHtml chưa được triển khai: thư viện sanitizer chưa chọn. Không render HTML do người dùng nhập cho tới khi hàm này có bản thật (xem shared/lib/sanitize-html.ts).",
  );
}
