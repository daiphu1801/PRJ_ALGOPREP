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

export const AUDIT_CATEGORIES: AuditCategory[] = ["auth", "permission", "config", "content"];

export type AuditEvent = {
  /** Event id shown to the operator, e.g. "evt_9f2a53". */
  id: string;
  /** Wall-clock time, HH:mm:ss. Kept as a string: the mock has no date component to format. */
  time: string;
  category: AuditCategory;
  message: string;
  /** Subsystem the action hit, e.g. "auth", "ai-config". */
  service: string;
  /** Username of the person who acted. Never a machine — see the note above. */
  actor: string;
};

export type AuditStat = {
  key: string;
  value: number;
  /** Change against the previous window, already signed. */
  delta: string;
  /** CSS custom-property name colouring the figure. */
  colorVar?: string;
};

export type ActiveAdmin = {
  name: string;
  /** Role plus action count, e.g. "Quản trị viên · 28 hành động". */
  meta: string;
  /** Relative time of their last action. */
  lastActionAt: string;
  colorVar?: string;
};

export type CategoryCount = { category: AuditCategory; count: number };

export type AuditLogPage = {
  stats: AuditStat[];
  events: AuditEvent[];
  activeAdmins: ActiveAdmin[];
  breakdown: CategoryCount[];
  /** Total events retained, for the "showing N of M" line. */
  totalEvents: number;
};

export function initialsOf(name: string): string {
  return name
    .split(" ")
    .slice(-2)
    .map((word) => word[0] ?? "")
    .join("");
}
