"use client";

import { useEffect, useState, type ReactNode } from "react";
import { isMockMode, tokenStore } from "@/shared/api";
import { refreshAccessToken } from "@/shared/api/client";

/** Restore the in-memory token before mounting API consumers. */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let active = true;
    const session =
      isMockMode() || tokenStore.get()
        ? Promise.resolve(tokenStore.get())
        : refreshAccessToken();
    void session.then((token) => {
      if (active) {
        tokenStore.set(token);
        setReady(true);
      }
    });
    return () => {
      active = false;
    };
  }, []);

  return ready ? children : null;
}
