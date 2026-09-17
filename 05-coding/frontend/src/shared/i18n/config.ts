// Bilingual vi/en is locked in (DEC-2026-0824-i18n-vi-en). No [locale] route segment —
// language switches via the NEXT_LOCALE cookie + data-ui-lang attribute on <html>, keeping the
// URL unchanged per 01-rd/system/frontend_architecture.md:34-37.
export const locales = ["vi", "en"] as const;
export type Locale = (typeof locales)[number];
export const defaultLocale: Locale = "vi";
export const localeCookieName = "NEXT_LOCALE";

export function isLocale(value: string | undefined): value is Locale {
  return locales.includes(value as Locale);
}
