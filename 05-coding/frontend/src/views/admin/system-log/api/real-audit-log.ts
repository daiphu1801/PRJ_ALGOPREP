// Real calls of the system-log screen (03-dd/api/F1_identity.md #39, #40, #64, #65, #51, #52; the live
// stream #66 is opened in live.ts). Any ADMIN reads and exports the log; only #52 needs the
// SYSTEM_AUDIT_LOG cell, and the server answers IDT-445 when it is missing.
import { api } from "@/shared/api";
import { formatRelativeVN } from "@/shared/lib";
import {
  AUDIT_CATEGORIES,
  type ActiveAdmin,
  type AuditCategory,
  type AuditEvent,
  type AuditLogParams,
  type AuditLogResultPage,
  type CategoryCount,
  type LogRetention,
} from "../model/types";

const BASE = "/identity/admin/audit-logs";
const RETENTION = "/identity/admin/settings/log-retention";

export type AuditLogItemDto = {
  id: string;
  createdAt: string;
  actorDisplayName: string;
  actorEmail: string;
  actionType: string;
  categoryCode: string;
  serviceCode: string;
  safeParams: Record<string, unknown> | null;
};

const UTC7_MS = 7 * 3_600_000;

/** The log reads in UTC+7 (DD ADM0403 section 6), whatever the browser's own zone is. */
function inUtc7(iso: string): { date: string; time: string } {
  const local = new Date(new Date(iso).getTime() + UTC7_MS).toISOString();
  return { date: local.slice(0, 10), time: local.slice(11, 19) };
}

function categoryOf(code: string): AuditCategory {
  const key = code.toLowerCase() as AuditCategory;
  // An unknown group falls back to the one the server itself falls back to (AuditActionCatalog).
  return AUDIT_CATEGORIES.includes(key) ? key : "config";
}

export function toAuditEvent(dto: AuditLogItemDto): AuditEvent {
  const params = dto.safeParams ?? {};
  const { date, time } = inUtc7(dto.createdAt);
  return {
    id: dto.id,
    shortId: dto.id.slice(0, 7),
    date,
    time,
    category: categoryOf(dto.categoryCode),
    action: { type: dto.actionType, params },
    service: dto.serviceCode,
    actor: dto.actorDisplayName,
    actorEmail: dto.actorEmail,
    reason:
      dto.actionType === "USER_LOCKED" && typeof params.reason === "string"
        ? params.reason
        : undefined,
  };
}

/** The filters that #39 and #40 share; an open end or "all" is simply not sent. */
function filterParams(params: AuditLogParams): Record<string, string> {
  const query = params.query.trim();
  return {
    ...(query ? { q: query } : {}),
    ...(params.category !== "all"
      ? { categoryCode: params.category.toUpperCase() }
      : {}),
    ...(params.from ? { dateFrom: params.from } : {}),
    ...(params.to ? { dateTo: params.to } : {}),
  };
}

/** #39: one page of 50 rows, newest first; the cursor of the previous page asks for the next. */
export async function realFetchAuditLogs(
  params: AuditLogParams,
  cursor: string | null,
): Promise<AuditLogResultPage> {
  const page = await api.get<{
    items: AuditLogItemDto[];
    nextCursor: string | null;
  }>(BASE, { ...filterParams(params), ...(cursor ? { cursor } : {}) });
  return { events: page.items.map(toAuditEvent), nextCursor: page.nextCursor };
}

/** #64: the five admins with the most actions in the last 24 hours. */
export async function realFetchActiveAdmins(): Promise<ActiveAdmin[]> {
  const { items } = await api.get<{
    items: {
      actorUserId: string;
      displayName: string;
      roleName: string;
      actionCount: number;
      lastActionAt: string;
    }[];
  }>(`${BASE}/active-admins`);
  return items.map((admin) => ({
    id: admin.actorUserId,
    name: admin.displayName,
    meta: `${admin.roleName} · ${admin.actionCount} hành động`,
    lastActionAt: formatRelativeVN(admin.lastActionAt),
  }));
}

/** #65: the four groups over the last seven days, zeros included. */
export async function realFetchCategoryBreakdown(): Promise<CategoryCount[]> {
  const { items } = await api.get<{
    items: { categoryCode: string; count: number }[];
  }>(`${BASE}/category-breakdown`);
  return items.map((item) => ({
    category: categoryOf(item.categoryCode),
    count: item.count,
  }));
}

export const EXPORT_COLUMNS = [
  "time",
  "actor",
  "service",
  "category",
  "content",
  "id",
] as const;
export type ExportColumn = (typeof EXPORT_COLUMNS)[number];

/** #40: the CSV as a Blob; the server streams it and writes the export into the log itself. */
export function realExportAuditLogs(input: {
  params: AuditLogParams;
  columns: readonly ExportColumn[];
  locale: "vi" | "en";
}): Promise<Blob> {
  return api.download(`${BASE}/export`, {
    params: {
      ...filterParams(input.params),
      columns: [...input.columns],
      format: "csv",
    },
    headers: { "Accept-Language": input.locale },
  });
}

type RetentionDto = {
  retentionDays: number;
  expiredPolicy: "DELETE" | "ARCHIVE";
};

function toRetention(dto: RetentionDto): LogRetention {
  return {
    retentionDays: dto.retentionDays,
    expiry: dto.expiredPolicy === "ARCHIVE" ? "archive" : "delete",
  };
}

/** #51. */
export async function realFetchLogRetention(): Promise<LogRetention> {
  return toRetention(await api.get<RetentionDto>(RETENTION));
}

/** #52: the server needs `SYSTEM_AUDIT_LOG`; the answer is the stored value. */
export async function realSaveLogRetention(
  input: LogRetention,
): Promise<LogRetention> {
  return toRetention(
    await api.put<RetentionDto>(RETENTION, {
      retentionDays: input.retentionDays,
      expiredPolicy: input.expiry === "archive" ? "ARCHIVE" : "DELETE",
    }),
  );
}
