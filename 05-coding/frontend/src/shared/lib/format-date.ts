// Fixed UTC+7 (Asia/Ho_Chi_Minh) date formatting, independent of the runtime timezone, so the
// server render, the browser and tests all print the same text. Accepts a Date, epoch ms or ISO string.

export type DateLocale = "vi" | "en";

const TIME_ZONE = "Asia/Ho_Chi_Minh";
const INTL_LOCALE: Record<DateLocale, string> = { vi: "vi-VN", en: "en-GB" };

const cache = new Map<string, Intl.DateTimeFormat>();

function formatter(locale: DateLocale, withTime: boolean) {
  const key = `${locale}:${withTime}`;
  let fmt = cache.get(key);
  if (!fmt) {
    fmt = new Intl.DateTimeFormat(INTL_LOCALE[locale], {
      timeZone: TIME_ZONE,
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      ...(withTime
        ? { hour: "2-digit", minute: "2-digit", hourCycle: "h23" as const }
        : {}),
    });
    cache.set(key, fmt);
  }
  return fmt;
}

/** "19/10/2026" (date) or "14:30 19/10/2026" (date-time), in UTC+7. Invalid input returns "". */
export function formatDateVN(
  value: Date | number | string,
  options: { locale?: DateLocale; withTime?: boolean } = {},
): string {
  const { locale = "vi", withTime = false } = options;
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const parts = Object.fromEntries(
    formatter(locale, withTime)
      .formatToParts(date)
      .map((p) => [p.type, p.value]),
  );
  const day = `${parts.day}/${parts.month}/${parts.year}`;
  return withTime ? `${parts.hour}:${parts.minute} ${day}` : day;
}
