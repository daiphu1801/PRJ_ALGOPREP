import type { ReactNode } from "react";

// Minimal layout for landing/login/register — no AppShell (no nav for a logged-in user).
// `grid-backdrop` (globals.css) rules the mockup's 48px blueprint grid behind the card; the Admin
// and Instructor screens paint their own opaque backdrop on top of it, so it only shows on /login
// and /register, which is exactly where the mockup has it.
export default function PublicLayout({ children }: { children: ReactNode }) {
  return <div className="grid-backdrop flex min-h-screen items-center justify-center">{children}</div>;
}
