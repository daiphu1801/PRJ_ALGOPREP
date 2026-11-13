"use client";

import {
  useEffect,
  useState,
  useSyncExternalStore,
  type ReactNode,
} from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { tokenStore } from "@/shared/api/token-store";

export function QueryProvider({ children }: { children: ReactNode }) {
  const session = useSyncExternalStore(
    tokenStore.subscribe,
    tokenStore.generation,
    () => 0,
  );
  return <SessionQueries key={session}>{children}</SessionQueries>;
}

/** Remount observers and local screen state too: previous-data placeholders cannot cross sessions. */
function SessionQueries({ children }: { children: ReactNode }) {
  const [client] = useState(() => new QueryClient());
  useEffect(
    () => () => {
      client.clear();
    },
    [client],
  );
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}
