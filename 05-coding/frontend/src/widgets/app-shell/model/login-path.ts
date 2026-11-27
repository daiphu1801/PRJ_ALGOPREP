import type { AppArea } from "@/entities/user";

/** Where each area sends a signed-out visitor, and where its sign-out entries lead. */
export const LOGIN_PATH_BY_AREA = {
  student: "/login",
  instructor: "/instructor/login",
  admin: "/admin/login",
} as const satisfies Record<Exclude<AppArea, "public">, string>;
