/**
 * System audit log (F1-14). ONE data source only: administrative actions taken by a human.
 * Infrastructure and service events (go-judge failures, worker disconnects, timeout sweeps) belong
 * to `judge-orchestration` (F4-10) and have their own screen, `admin_queue_monitor`. That boundary
 * is architectural, not a UX preference — merging the two sources needs a new decision
 * (02-bd/screens/admin/admin_system_log.md section 0).
 *
 * The "Chấm lại" (rejudge) category the static prototype still carries is out of scope entirely
 * (DEC-2026-0828-remove-rejudge-scope), so it is absent here — four categories remain.
 */
export type AuditCategory = "auth" | "permission" | "config" | "content";

export const AUDIT_CATEGORIES: AuditCategory[] = [
  "auth",
  "permission",
  "config",
  "content",
];

export type AuditEvent = {
  /** Row id (a UUID from the server): the React key and what the live stream dedupes on. */
  id: string;
  /** Short code shown to the operator: the first 7 characters of the UUID (DD ADM0403 BR-SC-01). */
  shortId: string;
  /** Calendar day in UTC+7, YYYY-MM-DD. Drives the "Từ ngày / Đến ngày" filter (ADM0403 Q6). */
  date: string;
  /** Wall-clock time in UTC+7, HH:mm:ss. */
  time: string;
  category: AuditCategory;
  /** Ready sentence of the prototype data; real rows have `action` instead. */
  message?: string;
  /** What the server sent: the screen builds the sentence from it (DD 0.7.2, BR-19). */
  action?: { type: string; params: Record<string, unknown> };
  /** Subsystem the action hit, e.g. "auth", "ai-config". */
  service: string;
  /** Display name of the person who acted. Never a machine, see the note above. */
  actor: string;
  /** E-mail of the actor (possibly the anonymised placeholder); what the server's text search reads. */
  actorEmail?: string;
  /**
   * Free-text reason the admin typed when locking an account (F1-32). Shown verbatim under the
   * message; an unlock row has none, which is how the two stay visually distinct (RD ADM0403 section 2
   * point 7).
   */
  reason?: string;
};

export type ActiveAdmin = {
  /** The admin's user id (real data); two admins can share a display name. */
  id?: string;
  name: string;
  /** Role plus action count, e.g. "Quản trị viên · 28 hành động". */
  meta: string;
  /** Relative time of their last action. */
  lastActionAt: string;
  colorVar?: string;
};

export type CategoryCount = { category: AuditCategory; count: number };

/** The filters of the list, as the screen holds them (an empty string is an open end). */
export type AuditLogParams = {
  query: string;
  category: AuditCategory | "all";
  from: string;
  to: string;
};

/** One keyset page of the list; `nextCursor` is null on the last page. */
export type AuditLogResultPage = {
  events: AuditEvent[];
  nextCursor: string | null;
};

/** Retention of the log (F1 #51, #52). */
export type LogRetention = {
  retentionDays: number;
  expiry: "delete" | "archive";
};

export function initialsOf(name: string): string {
  return name
    .split(" ")
    .slice(-2)
    .map((word) => word[0] ?? "")
    .join("");
}
