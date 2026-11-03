import type { ReactNode } from "react";
import { AppShell, RoleGate } from "@/widgets/app-shell";

export default function InstructorLayout({
  children,
}: {
  children: ReactNode;
}) {
  return (
    <RoleGate area="instructor">
      <AppShell area="instructor">{children}</AppShell>
    </RoleGate>
  );
}
