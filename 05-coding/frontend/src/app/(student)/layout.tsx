import type { ReactNode } from "react";
import { AppShell } from "@/widgets/app-shell";

// Bảo vệ route thật (kiểm session/role) là việc của middleware.ts — chưa dựng vì chưa có
// endpoint xác thực thật (03-dd/api/identity.md chưa có). Layout này chỉ lắp AppShell.
export default function StudentLayout({ children }: { children: ReactNode }) {
  return <AppShell area="student">{children}</AppShell>;
}
