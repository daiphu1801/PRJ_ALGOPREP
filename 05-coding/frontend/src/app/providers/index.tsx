"use client";

import type { ReactNode } from "react";
import { QueryProvider } from "./query-provider";
import { ThemeProvider } from "./theme-provider";
import { AuthProvider } from "./auth-provider";
import { AppToaster } from "./app-toaster";

export function Providers({ children }: { children: ReactNode }) {
  return (
    <ThemeProvider>
      <AuthProvider>
        <QueryProvider>
          {children}
          <AppToaster />
        </QueryProvider>
      </AuthProvider>
    </ThemeProvider>
  );
}
