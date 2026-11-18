// Real call for `GET /identity/instructor/dashboard/recent-activity` (03-dd/api/F1_identity.md #45,
// "Trien khai 2026-11-05"). The server sends the actor and the activity type, never a sentence and
// never a problem title (identity does not read problem-bank), so the screen words the line from
// `activityType` through i18n; `text` only holds the actor name as a fallback for an unknown type.
import { api } from "@/shared/api";
import { formatRelativeVN } from "@/shared/lib";
import type { ClassActivity } from "../model/types";

type ActivityDto = {
  id: string;
  activityType: string;
  actorDisplayName: string;
  occurredAt: string;
};

export async function realListClassActivity(): Promise<ClassActivity[]> {
  const { items } = await api.get<{ items: ActivityDto[] }>(
    "/identity/instructor/dashboard/recent-activity",
  );
  return items.map((item) => ({
    id: item.id,
    activityType: item.activityType,
    actorName: item.actorDisplayName,
    text: item.actorDisplayName,
    timeLabel: formatRelativeVN(item.occurredAt),
    attention: false,
  }));
}
