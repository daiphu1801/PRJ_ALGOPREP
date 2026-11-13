import type { ReactNode } from "react";
import { AppShell, RoleGate } from "@/widgets/app-shell";

// RoleGate only redirects (UX). The backend enforces access on every request.
export default function StudentLayout({ children }: { children: ReactNode }) {
  return (
    <RoleGate area="student">
      <AppShell area="student">{children}</AppShell>
    </RoleGate>
  );
}
