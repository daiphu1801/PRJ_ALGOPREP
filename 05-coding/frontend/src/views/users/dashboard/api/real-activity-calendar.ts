// Real call for `GET /identity/me/activity-calendar` (03-dd/api/F1_identity.md #55, section 0.8.1).
// The server sends exactly 364 zero-filled days with only `submissionCount`; the shading level and
// the active-day count are display concerns of this screen (BD USR0601 Khu vuc E NO 2 and NO 4).
import { api } from "@/shared/api";
import type { ActivityCalendar, ActivityDay } from "../model/types";

type CalendarDto = {
  from: string;
  to: string;
  days: { day: string; submissionCount: number }[];
};

const DAYS_PER_WEEK = 7;
const MONTHS_ON_AXIS = 12;

// [SoT: Suy luan] thresholds, the BD leaves them to the screen. The mock draws 2, 4, 6 and 8
// submissions for levels 1 to 4, and these bands keep that look.
export function levelOf(count: number): ActivityDay["level"] {
  if (count <= 0) return 0;
  if (count <= 2) return 1;
  if (count <= 4) return 2;
  if (count <= 7) return 3;
  return 4;
}

export async function realGetActivityCalendar(): Promise<ActivityCalendar> {
  const dto = await api.get<CalendarDto>("/identity/me/activity-calendar");
  const cells: ActivityDay[] = dto.days.map((d) => ({
    day: d.day,
    count: d.submissionCount,
    level: levelOf(d.submissionCount),
  }));

  const weeks: ActivityDay[][] = [];
  for (let i = 0; i < cells.length; i += DAYS_PER_WEEK) {
    weeks.push(cells.slice(i, i + DAYS_PER_WEEK));
  }

  // One label per distinct month of a week's last day, oldest first, newest 12 kept.
  const months: string[] = [];
  for (const week of weeks) {
    const last = week[week.length - 1];
    if (!last) continue;
    const label = `T${Number(last.day.slice(5, 7))}`;
    if (months[months.length - 1] !== label) months.push(label);
  }

  return {
    weeks,
    monthLabels: months.slice(-MONTHS_ON_AXIS),
    activeDayCount: cells.filter((c) => c.count > 0).length,
  };
}
