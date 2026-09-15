import type { Role } from "./types";

/**
 * Application routing area, matching the route groups under app/:
 * (public) · (student) · (instructor)/instructor/* · (admin)/admin/*
 */
export type AppArea = "public" | "student" | "instructor" | "admin";

/**
 * The role REQUIRED to enter each area. This is the single source of truth for that rule —
 * middleware.ts (not built yet, needs 03-dd/api/identity.md) must read this map instead of
 * duplicating it, otherwise the two will drift and an area will end up unguarded without anyone
 * noticing in a diff.
 *
 * SEMANTIC NOTE: this is the role REQUIRED by the area, NOT the role of the currently logged-in
 * user. Real authorization is enforced by the backend; blocking on the client is only UX
 * (01-rd/system/frontend_architecture.md section 4).
 */
export const ROLE_BY_AREA = {
  student: "STUDENT",
  instructor: "INSTRUCTOR",
  admin: "ADMIN",
} as const satisfies Record<Exclude<AppArea, "public">, Role>;

/** Post-login navigation target, by role — locked in at 01-rd/screens/shared/auth.md:90 (Q3). */
export const HOME_PATH_BY_ROLE = {
  STUDENT: "/progress",
  INSTRUCTOR: "/instructor/overview",
  // Unblocked 2026-08-25: `admin_overview` has a real prototype (09-layoutBase/Admin - Tổng
  // quan.dc.html) and was added to system_survey.md section 7.3 after the Admin area reconciliation pass.
  ADMIN: "/admin/overview",
} as const satisfies Record<Role, string>;
