"use client";

import { useEffect } from "react";
import { usePathname } from "next/navigation";
import { authLoading } from "../model/loading-store";

/**
 * Drops the post-login overlay once the app has moved off the sign-in page. The overlay stays up
 * through the navigation on purpose, so the sign-in form does not flash back before the home
 * screen is ready. Mounted once, outside the session-keyed subtree.
 */
export function AuthLoadingReset() {
  const pathname = usePathname();
  useEffect(() => {
    authLoading.clearIfLeft(pathname);
  }, [pathname]);
  return null;
}
