import type { ReactNode } from "react";

// Layout tối giản cho landing/login/register — không có AppShell (không có nav người dùng đã
// đăng nhập).
export default function PublicLayout({ children }: { children: ReactNode }) {
  return <div className="flex min-h-screen items-center justify-center">{children}</div>;
}
