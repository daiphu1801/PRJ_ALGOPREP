// Real call for the "Bai nop theo ngay" chart: F4 `GET /judge/me/metrics?range=` field `dailyCounts`
// (03-dd/api/F4_judge-orchestration.md section 12; 03-dd/api/F1_identity.md 0.8.1 keeps this source
// instead of #55). The server already sends every column: 7 days for "7d", 14 groups for "30d", one
// ISO week (Monday) per column for "all"; this only words the label as dd/MM.
import { fetchMyMetrics } from "@/entities/progress";
import type { DashboardDailyPoint, DashboardRange } from "../model/types";

export async function realGetDashboardDaily(
  range: DashboardRange,
): Promise<DashboardDailyPoint[]> {
  const { dailyCounts } = await fetchMyMetrics(range);
  return dailyCounts.map(({ day, count }) => ({
    // `day` is YYYY-MM-DD already in UTC+7; slicing avoids a timezone shift from Date parsing.
    label: `${day.slice(8, 10)}/${day.slice(5, 7)}`,
    count,
  }));
}
