import type { ReactNode } from "react";
import { NextIntlClientProvider } from "next-intl";
import { getLocale, getMessages, getTranslations } from "next-intl/server";

export { getTranslations as getT, getLocale };

/**
 * The only facade allowed to import next-intl directly (blocked everywhere else via ESLint
 * no-restricted-imports) — swapping the i18n library later only means editing this file.
 */
export async function IntlProvider({ children }: { children: ReactNode }) {
  const locale = await getLocale();
  const messages = await getMessages();

  return (
    <NextIntlClientProvider locale={locale} messages={messages}>
      {children}
    </NextIntlClientProvider>
  );
}
