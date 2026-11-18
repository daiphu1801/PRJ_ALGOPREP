import type { ReactNode } from "react";
import { AppShell, RoleGate } from "@/widgets/app-shell";

export default function AdminLayout({ children }: { children: ReactNode }) {
  return (
    <RoleGate area="admin">
      <AppShell area="admin">{children}</AppShell>
    </RoleGate>
  );
}
