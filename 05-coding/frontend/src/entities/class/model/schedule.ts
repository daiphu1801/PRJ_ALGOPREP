// One-line text of a class schedule, e.g. "T2 · T4 · T6 — 19:00–21:00 · B203 · 17/08–20/12 · ghi chú".
// The weekday labels come from the caller (i18n), so the same function serves both languages.
import type { ClassSchedule } from "./types";

const dayMonth = (iso: string) => {
  const [, month, day] = iso.split("-");
  return month && day ? `${day}/${month}` : iso;
};

export function formatClassSchedule(
  schedule: ClassSchedule | undefined,
  dayLabel: (day: number) => string,
): string {
  if (!schedule) return "";
  const days = [...schedule.days]
    .sort((a, b) => a - b)
    .map(dayLabel)
    .join(" · ");
  const time = schedule.startTime
    ? schedule.endTime
      ? `${schedule.startTime}–${schedule.endTime}`
      : schedule.startTime
    : "";
  const first = [days, time].filter(Boolean).join(" — ");
  const period =
    schedule.startDate && schedule.endDate
      ? `${dayMonth(schedule.startDate)}–${dayMonth(schedule.endDate)}`
      : schedule.startDate
        ? dayMonth(schedule.startDate)
        : "";
  return [first, schedule.room, period, schedule.note]
    .filter((part): part is string => Boolean(part))
    .join(" · ");
}
