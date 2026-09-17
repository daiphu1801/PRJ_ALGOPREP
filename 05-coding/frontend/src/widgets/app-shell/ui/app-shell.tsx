import type { ReactNode } from "react";
import type { AppArea } from "@/entities/user";
import { getT } from "@/shared/i18n/server";
import { LiquidGlassBackdrop } from "@/shared/ui";
import { AdminSidebar } from "./admin-sidebar";
import { AdminToolbar } from "./admin-toolbar";

type AppShellProps = {
  /** Routing area, NOT the role of the currently logged-in user (see entities/user/model/area.ts). */
  area: Exclude<AppArea, "public">;
  children: ReactNode;
};

/**
 * Shared application shell. `student`/`instructor` still get the plain header-only shell (their
 * 02-bd/screens/ layout isn't locked in yet). `admin` now gets the real shell per
 * 02-bd/screens/admin/admin_overview.md section 2 point 1: a sidebar (11 nav destinations, shared
 * across ALL Admin screens, not specific to admin_overview) + toolbar — this is why the sidebar
 * was added HERE instead of a one-off widget under views/admin-overview (it must show up on the
 * other 10 stub Admin pages too). Ran `impact` on AppShell before this edit
 * (mcp__gitnexus__impact, upstream, repo PRJ_ALGOPREP): 3 direct callers
 * (AdminLayout/InstructorLayout/StudentLayout), risk LOW — the branch below only changes the
 * `admin` path, the other two callers render byte-identical output to before.
 *
 * The real user name and role will show up in AdminToolbar once a session exists
 * (app/providers/auth-provider.tsx is still a TODO) — for now it only shows a placeholder so it
 * doesn't pretend to know who's logged in.
 */
export async function AppShell({ area, children }: AppShellProps) {
  if (area === "admin") {
    // Sidebar + main are a flex ROW from the very top — no page-wide header sitting above them.
    // dc.html:55-101 puts `<aside>` and `<main>` as direct siblings of one flex row spanning the
    // full 100vh; the icon/user row (dc.html:106-122) is the FIRST child INSIDE `<main>`'s own
    // padded content, not a separate bar spanning above the sidebar. Getting this wrong (a
    // full-width `<header>` above the row) was reported by the owner as "bỏ header đi" — pushes
    // the sidebar down instead of letting it run the full viewport height.
    return (
      <div className="admin-shell relative flex min-h-screen bg-[var(--color-background)] text-[var(--color-text)]">
        <LiquidGlassBackdrop />
        <AdminSidebar />
        <main className="min-w-0 flex-1 overflow-y-auto p-4">
          {/* Shared by ALL 11 Admin screens (not just admin_overview) — matches dc.html:104
              `max-width: 1320px` on the inner content wrapper, which every screen used but this
              app never applied, so wide viewports stretched cards out with no cap. One container
              here fixes it everywhere instead of each view repeating its own max-width. */}
          <div className="mx-auto w-full max-w-[1320px]">
            <AdminToolbar />
            {children}
          </div>
        </main>
      </div>
    );
  }

  const t = await getT("nav");

  return (
    <div className="flex min-h-screen flex-col bg-[var(--color-background)] text-[var(--color-text)]">
      <header className="glass-surface flex h-14 items-center justify-between border-b border-[var(--color-border)] px-4">
        <span className="font-semibold">AlgoPrep</span>
        <span className="text-sm text-[var(--color-text-muted)]">{t(`area.${area}`)}</span>
      </header>
      <main className="flex-1">{children}</main>
    </div>
  );
}
