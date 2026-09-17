// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Sticky screen header: title + subtitle on the left, screen-level actions on the right.
// 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:146-159 — 62px tall, sticky at 18px, 20px
// radius, glass fill, with the primary CTA at the right end.
//
// Not part of widgets/app-shell: `admin_overview` deliberately has NO visible title (its mockup
// goes straight from the toolbar into the grid, see admin-overview-view.tsx), so a header belongs
// to the screen that wants one rather than to the shell every screen shares.
//
// The theme switch that the mockup also parks in this bar is NOT repeated here — this app keeps a
// single ThemeLangSwitcher in the sidebar (admin-sidebar.tsx), and two independent copies of the
// same control on one screen is a worse result than the mockup's redundancy.
import type { ReactNode } from "react";
import { cn } from "@/shared/lib";

type PageHeaderProps = {
  title: ReactNode;
  description?: ReactNode;
  /** Screen-level actions, e.g. a primary save button. */
  actions?: ReactNode;
  className?: string;
};

export function PageHeader({ title, description, actions, className }: PageHeaderProps) {
  return (
    <header
      className={cn(
        "glass-card sticky top-0 z-10 mb-4 flex min-h-[62px] items-center gap-3 border border-[var(--color-border)] px-4 py-2.5",
        className,
      )}
    >
      <div className="min-w-0 flex-1">
        <h1 className="truncate text-base font-bold tracking-tight">{title}</h1>
        {description ? (
          <p className="truncate text-[12.5px] text-[var(--color-text-muted)]">{description}</p>
        ) : null}
      </div>
      {actions ? <div className="flex shrink-0 items-center gap-2">{actions}</div> : null}
    </header>
  );
}
