import type { ReactNode } from "react";

// Minimal layout for landing/login/register — no AppShell (no nav for a logged-in user).
export default function PublicLayout({ children }: { children: ReactNode }) {
  return <div className="flex min-h-screen items-center justify-center">{children}</div>;
}
