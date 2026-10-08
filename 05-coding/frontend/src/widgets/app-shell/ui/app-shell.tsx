import type { ReactNode } from "react";
import type { AppArea } from "@/entities/user";
import { getT } from "@/shared/i18n/server";
import {
  LiquidGlassBackdrop,
  MAIN_CONTENT_ID,
  PageContainer,
  SkipLink,
} from "@/shared/ui";
import { SHELL_MAX_WIDTH_PX } from "@/shared/api/config";
import { AdminFooter } from "./admin-footer";
import { AdminSidebar } from "./admin-sidebar";
import { AdminToolbar } from "./admin-toolbar";
import { InstructorSidebar } from "./instructor-sidebar";
import { StudentFooter } from "./student-footer";
import { StudentHeader } from "./student-header";

type AppShellProps = {
  /** Routing area, NOT the role of the currently logged-in user (see entities/user/model/area.ts). */
  area: Exclude<AppArea, "public">;
  children: ReactNode;
};

/**
 * Shared application shell. Each authenticated area gets its own chrome, because each one has
 * its own prototype: `instructor` a sidebar + blob backdrop (added 2026-09-27), `student` a
 * top-nav header (added 2026-09-27, see the `student` branch below). `admin` gets the real shell per
 * 02-bd/screens/admin/ADM0101_overview.md section 2 point 1: a sidebar (11 nav destinations, shared
 * across ALL Admin screens, not specific to admin_overview) + toolbar — this is why the sidebar
 * was added HERE instead of a one-off widget under views/admin-overview (it must show up on the
 * other 10 stub Admin pages too). Ran `impact` on AppShell before this edit
 * (mcp__gitnexus__impact, upstream, repo PRJ_ALGOPREP): 3 direct callers
 * (AdminLayout/InstructorLayout/StudentLayout), risk LOW — each caller passes a fixed `area`
 * literal, so a branch only ever changes the one layout that selects it.
 *
 * All three areas now have their own branch, so the generic header-only fallback that used to sit
 * at the end is gone — `area` is narrowed to `never` there, which is what TS reported once the
 * `instructor` branch landed.
 *
 * The real user name and role will show up in AdminToolbar once a session exists
 * (app/providers/auth-provider.tsx is still a TODO) — for now it only shows a placeholder so it
 * doesn't pretend to know who's logged in.
 */
export async function AppShell({ area, children }: AppShellProps) {
  if (area === "admin") {
    const t = await getT("adminNav");
    // Sidebar + main are a flex ROW from the very top — no page-wide header sitting above them.
    // dc.html:55-101 puts `<aside>` and `<main>` as direct siblings of one flex row spanning the
    // full 100vh; the icon/user row (dc.html:106-122) is the FIRST child INSIDE `<main>`'s own
    // padded content, not a separate bar spanning above the sidebar. Getting this wrong (a
    // full-width `<header>` above the row) was reported by the owner as "bỏ header đi" — pushes
    // the sidebar down instead of letting it run the full viewport height.
    return (
      <div className="admin-shell relative flex min-h-screen bg-[var(--color-background)] text-[var(--color-text)]">
        <SkipLink label={t("skipToContent")} />
        <LiquidGlassBackdrop />
        <AdminSidebar />
        <main
          id={MAIN_CONTENT_ID}
          tabIndex={-1}
          className="min-w-0 flex-1 overflow-y-auto py-4 outline-none"
        >
          {/* Shared by ALL 11 Admin screens (not just admin_overview) — dc.html:104 caps the inner
              content wrapper at 1320px. The cap now comes from shared/config/layout.ts through the
              same PageContainer the other two areas use, so all three are tuned in one place and
              share one responsive gutter (owner instruction 2026-09-27). */}
          <PageContainer maxWidthPx={SHELL_MAX_WIDTH_PX.admin}>
            <AdminToolbar />
            {children}
            {/* Shared by ALL 11 Admin screens, decided 2026-09-21 (02-bd/screens/admin/_shell.md
                section 9) — deliberately overrides admin_overview's own footerless prototype so the
                whole Admin area has one consistent chrome instead of one screen missing it. */}
            <AdminFooter />
          </PageContainer>
        </main>
      </div>
    );
  }

  if (area === "student") {
    const t = await getT("studentShell");
    // Header, footer and content cap all belong to the AREA, not to any one screen: every Student
    // prototype repeats the same header block, and 02-bd/screens/users/_shell.md mục 3 puts the
    // footer on every screen of the area.
    return (
      // No LiquidGlassBackdrop here: those 3 colored blobs are a 1:1 port of the ADMIN prototype
      // (Admin - Tổng quan.dc.html:51-53). Every Student prototype instead sits on a plain neutral
      // canvas, so borrowing the admin blobs would paint in colors the Student screens never had.
      <div className="flex min-h-screen flex-col bg-[var(--color-background)] text-[var(--color-text)]">
        <SkipLink label={t("skipToContent")} />
        <StudentHeader />
        {/* Header height published as a custom property so full-height views (Workspace,
            Mock Interview) can size themselves against the real chrome instead of each
            hard-coding its own guess — 14px top pad + 58px bar + 12px bottom pad. */}
        <main
          id={MAIN_CONTENT_ID}
          tabIndex={-1}
          className="flex-1 outline-none [--app-header-h:84px]"
        >
          <PageContainer maxWidthPx={SHELL_MAX_WIDTH_PX.student}>
            {children}
          </PageContainer>
        </main>
        {/* OUTSIDE `<main>` on purpose: a `<footer>` nested inside main/section/article is not
            exposed as the `contentinfo` landmark, so a screen reader loses the "page footer"
            jump target. It still goes through the same PageContainer so it lines up with the
            content above it. */}
        <PageContainer maxWidthPx={SHELL_MAX_WIDTH_PX.student}>
          <StudentFooter />
        </PageContainer>
      </div>
    );
  }

  // Sidebar and main are one flex row from the top, with no header above them — same structure as
  // the Admin branch, and for the same reason: 09-layoutBase/Giáo viên - Tổng quan.dc.html:61-63
  // makes `<aside>` and `<main>` direct siblings of a single 100vh flex row. The search/CTA bar
  // the overview mockup shows (:113-126) is the first child INSIDE `<main>`, so it stays in
  // instructor-overview-view.tsx — the other 4 Instructor screens do not have it.
  //
  // `.instructor-shell` is what makes these screens render in the mockup's blue palette instead
  // of the app's generic placeholder tokens (globals.css). LiquidGlassBackdrop is reused as-is:
  // it reads --color-blob-1/2/3, which the scope redefines to this mockup's own blob colors
  // (dc.html:29) — unlike the Student area, the Instructor prototypes really do draw the three
  // blurred circles (:56-58).
  const t = await getT("instructorNav");
  return (
    <div className="instructor-shell relative flex min-h-screen bg-[var(--color-background)] text-[var(--color-text)]">
      <SkipLink label={t("skipToContent")} />
      <LiquidGlassBackdrop />
      <InstructorSidebar />
      <main
        id={MAIN_CONTENT_ID}
        tabIndex={-1}
        className="min-w-0 flex-1 overflow-y-auto py-[18px] outline-none"
      >
        {/* dc.html:112 caps the content wrapper at 1320px on every Instructor screen — same shared
            container and same config entry as the other two areas. */}
        <PageContainer maxWidthPx={SHELL_MAX_WIDTH_PX.instructor}>
          {children}
        </PageContainer>
      </main>
    </div>
  );
}
