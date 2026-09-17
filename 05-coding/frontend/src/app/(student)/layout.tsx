import type { ReactNode } from "react";
import { AppShell } from "@/widgets/app-shell";

// Real route protection (session/role check) is middleware.ts's job — not built yet because
// there's no real auth endpoint (03-dd/api/identity.md doesn't exist). This layout only wires up AppShell.
export default function StudentLayout({ children }: { children: ReactNode }) {
  return <AppShell area="student">{children}</AppShell>;
}
