"use client";

import { useCallback } from "react";
import { useRouter } from "next/navigation";
import { logout } from "@/entities/auth";

/** The menu entry key that signs the user out instead of just navigating. */
export const LOGOUT_ITEM_KEY = "logout";

/**
 * Ends the session on the server (revokes the refresh token and clears its cookie), then goes to
 * `href`. The person is sent away even if the call fails: they asked to leave, and the in-memory
 * access token is already dropped by `logout`.
 */
export function useSignOut(): (href: string) => Promise<void> {
  const router = useRouter();
  return useCallback(
    async (href: string) => {
      await logout().catch(() => undefined);
      router.push(href);
    },
    [router],
  );
}
