"use client";

import type { ReactNode } from "react";
import { QueryProvider } from "./query-provider";
import { ThemeProvider } from "./theme-provider";
import { AuthProvider } from "./auth-provider";
import { AppToaster } from "./app-toaster";
import { AuthLoadingReset } from "@/features/auth-by-credentials";

export function Providers({
  children,
  nonce,
}: {
  children: ReactNode;
  nonce?: string;
}) {
  return (
    <ThemeProvider nonce={nonce}>
      <AuthProvider>
        <QueryProvider>
          {children}
          <AppToaster />
        </QueryProvider>
        <AuthLoadingReset />
      </AuthProvider>
    </ThemeProvider>
  );
}
