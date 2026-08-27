"use client";

export { useTranslations as useT, useLocale } from "next-intl";
export { locales, defaultLocale, localeCookieName, type Locale } from "./config";

import { localeCookieName, type Locale } from "./config";

/**
 * Đổi ngôn ngữ mà KHÔNG đổi URL: ghi cookie NEXT_LOCALE, đồng bộ attribute data-ui-lang
 * trên <html>, rồi reload để Server Component đọc lại locale (xem shared/i18n/request.ts).
 */
export function setLocale(locale: Locale): void {
  // SameSite=Lax: cookie này chỉ mang tuỳ chọn ngôn ngữ (không phải thông tin xác thực), nhưng
  // vẫn đặt SameSite để không gửi kèm trong request cross-site — mặc định của trình duyệt khác
  // nhau và một cookie không khai SameSite là một dòng cảnh báo trong mọi bản audit bảo mật.
  document.cookie = `${localeCookieName}=${locale}; path=/; max-age=31536000; SameSite=Lax`;
  document.documentElement.setAttribute("data-ui-lang", locale);
  document.documentElement.lang = locale;
  window.location.reload();
}
