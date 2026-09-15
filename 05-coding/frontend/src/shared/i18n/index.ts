"use client";

export { useTranslations as useT, useLocale } from "next-intl";
export { locales, defaultLocale, localeCookieName, type Locale } from "./config";

import { localeCookieName, type Locale } from "./config";

/**
 * Switches language WITHOUT changing the URL: writes the NEXT_LOCALE cookie, syncs the
 * data-ui-lang attribute on <html>, then reloads so the Server Component re-reads the locale
 * (see shared/i18n/request.ts).
 */
export function setLocale(locale: Locale): void {
  // SameSite=Lax: this cookie only carries a language preference (not auth info), but we still
  // set SameSite so it isn't sent on cross-site requests — browser defaults differ, and a
  // cookie with no declared SameSite is a flagged line in every security audit.
  document.cookie = `${localeCookieName}=${locale}; path=/; max-age=31536000; SameSite=Lax`;
  document.documentElement.setAttribute("data-ui-lang", locale);
  document.documentElement.lang = locale;
  window.location.reload();
}
