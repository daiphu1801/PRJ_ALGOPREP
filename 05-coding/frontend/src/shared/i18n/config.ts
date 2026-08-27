// Song ngữ vi/en đã chốt (DEC-2026-0824-i18n-vi-en). Không dùng route segment [locale] —
// ngôn ngữ đổi qua cookie NEXT_LOCALE + attribute data-ui-lang trên <html>, giữ URL nguyên
// theo 01-rd/system/frontend_architecture.md:34-37.
export const locales = ["vi", "en"] as const;
export type Locale = (typeof locales)[number];
export const defaultLocale: Locale = "vi";
export const localeCookieName = "NEXT_LOCALE";

export function isLocale(value: string | undefined): value is Locale {
  return locales.includes(value as Locale);
}
