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

const MINUTE = 60_000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;
const relativeCache = new Map<string, Intl.RelativeTimeFormat>();

/**
 * "5 phút trước", "hôm qua", "3 ngày trước" for a moment in the last 30 days; older moments fall back
 * to the date in UTC+7. A moment in the future reads as "now". Invalid input returns "".
 */
export function formatRelativeVN(
  value: Date | number | string,
  options: { locale?: DateLocale; now?: number } = {},
): string {
  const { locale = "vi", now = Date.now() } = options;
  const time = (value instanceof Date ? value : new Date(value)).getTime();
  if (Number.isNaN(time)) return "";
  let rtf = relativeCache.get(locale);
  if (!rtf) {
    rtf = new Intl.RelativeTimeFormat(INTL_LOCALE[locale], { numeric: "auto" });
    relativeCache.set(locale, rtf);
  }
  const age = now - time;
  if (age < MINUTE) return rtf.format(0, "second");
  if (age < HOUR) return rtf.format(-Math.floor(age / MINUTE), "minute");
  if (age < DAY) return rtf.format(-Math.floor(age / HOUR), "hour");
  if (age < 30 * DAY) return rtf.format(-Math.floor(age / DAY), "day");
  return formatDateVN(time, { locale });
}
