"use client";

import { useEffect, type ReactNode } from "react";
import { usePathname, useRouter } from "next/navigation";
import {
  HOME_PATH_BY_ROLE,
  ROLE_BY_AREA,
  type AppArea,
} from "@/entities/user";
import { isMockMode, tokenStore } from "@/shared/api";
import { readBaseCategory } from "@/shared/lib";
import { LOGIN_PATH_BY_AREA } from "../model/login-path";

/**
 * Sends the visitor away from an area their account may not use, so typing `/admin/...` as a
 * student, or signed out, does not show an admin page full of failing calls.
 *
 * This is navigation only. Authorization is enforced by the backend on every request (JWT plus
 * `FUNCTION:ACTION`), and nothing here is trusted: the role comes from decoding the token the
 * backend issued, not from a signed check. Mock mode lets everyone through because the mock has no
 * real session.
 *
 * `AuthProvider` renders nothing until it has tried to restore the session, so by the time this
 * mounts the in-memory token is already set.
 */
export function RoleGate({
  area,
  children,
}: {
  area: Exclude<AppArea, "public">;
  children: ReactNode;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const required = ROLE_BY_AREA[area];
  // Safe to read during render: AuthProvider renders nothing until the session is restored.
  const role = isMockMode() ? required : readBaseCategory(tokenStore.get());
  const allowed = role === required;

  useEffect(() => {
    if (allowed) return;
    router.replace(
      role
        ? HOME_PATH_BY_ROLE[role]
        : `${LOGIN_PATH_BY_AREA[area]}?redirect=${encodeURIComponent(pathname)}`,
    );
  }, [allowed, area, pathname, role, router]);

  return allowed ? children : null;
}
