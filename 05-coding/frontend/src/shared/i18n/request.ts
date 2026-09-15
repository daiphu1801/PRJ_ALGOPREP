import { getRequestConfig } from "next-intl/server";
import { cookies } from "next/headers";
import { defaultLocale, isLocale, localeCookieName } from "./config";

// next-intl mode does NOT use routing/[locale] — the locale is read from a cookie, not the URL.
export default getRequestConfig(async () => {
  const cookieStore = await cookies();
  const cookieLocale = cookieStore.get(localeCookieName)?.value;
  const locale = isLocale(cookieLocale) ? cookieLocale : defaultLocale;

  const messages = (await import(`../../../messages/${locale}.json`)).default;

  return { locale, messages };
});
