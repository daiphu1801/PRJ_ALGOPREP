import type { ReactNode } from "react";
import { AppShell } from "@/widgets/app-shell";

export default function InstructorLayout({ children }: { children: ReactNode }) {
  return <AppShell area="instructor">{children}</AppShell>;
}
