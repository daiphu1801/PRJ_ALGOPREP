import type { ReactNode } from "react";
import { NextIntlClientProvider } from "next-intl";
import { getLocale, getMessages, getTranslations } from "next-intl/server";

export { getTranslations as getT, getLocale };

/**
 * Facade duy nhất được phép import next-intl trực tiếp (chặn ở nơi khác bằng ESLint
 * no-restricted-imports) — đổi thư viện i18n sau này chỉ sửa file này.
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
