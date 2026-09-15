// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useEffect, useState } from "react";
import { usePathname } from "next/navigation";
import { NavGroupHeader, NavLink, ThemeLangSwitcher } from "@/shared/ui";
import { useT } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { ADMIN_NAV_GROUPS, ADMIN_NAV_MISC, ADMIN_NAV_OVERVIEW } from "../model/admin-nav";

/**
 * Layout matches 09-layoutBase/Admin - Tổng quan.dc.html:57-101 1:1 (02-bd/screens/admin/admin_overview.md
 * section 2 point 1): logo + collapse toggle live INSIDE the sidebar (not the toolbar), and the
 * theme switcher sits at the sidebar's footer — the static prototype has no toolbar branding at
 * all. Clicking a group header only expands/collapses it — it does NOT navigate and does NOT leave
 * admin_overview (BD section 6, last bullet).
 *
 * Collapse state persists like the prototype's `algoprep-admin-collapsed` localStorage key (dc.html
 * line 308), read via `localStorage` guarded in try/catch — this is a per-viewer UI convenience,
 * not app state, so localStorage is an acceptable prototype shortcut here (unlike the access token,
 * which must never go there).
 */
export function AdminSidebar() {
  const t = useT("adminNav");
  const pathname = usePathname();
  // Two independent sources decide the visual state, not one: `collapsed` is the user's own
  // persisted preference (toggle button, localStorage); `isNarrowViewport` is a live viewport
  // check (below `lg`, 1024px — same cutoff the dashboard grids already use, e.g.
  // views/admin-overview/ui/admin-overview-view.tsx `lg:grid-cols-*`). A narrow viewport always
  // forces the icon-rail width regardless of the saved preference — the owner flagged the sidebar
  // as "bị dãn quá" (too stretched) on 2026-09-14, i.e. it never got any narrower on smaller
  // windows. Reused as-is by every Admin screen since this lives in the shared shell, not a
  // per-screen component.
  const [collapsed, setCollapsed] = useState(false);
  const [isNarrowViewport, setIsNarrowViewport] = useState(false);
  const effectiveCollapsed = collapsed || isNarrowViewport;

  useEffect(() => {
    const mediaQuery = window.matchMedia("(max-width: 1023px)");
    const applyFromViewport = () => setIsNarrowViewport(mediaQuery.matches);
    applyFromViewport();
    mediaQuery.addEventListener("change", applyFromViewport);
    return () => mediaQuery.removeEventListener("change", applyFromViewport);
  }, []);

  // Independent per-group open state — deliberately NOT a single-open accordion. dc.html:302, 332
  // only allows one group open at a time (a single `openGroup` var), but the owner asked on
  // 2026-09-14 to let multiple parent groups stay expanded together, so this is one intentional
  // divergence from 1:1 prototype fidelity. Defaults to all closed, same as the mockup.
  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>({});

  const toggleGroup = (key: string) => setOpenGroups((prev) => ({ ...prev, [key]: !prev[key] }));
  const toggleCollapsed = () => {
    setCollapsed((prev) => {
      const next = !prev;
      try {
        localStorage.setItem("algoprep-admin-collapsed", next ? "1" : "0");
      } catch {
        // Private window / blocked storage — collapse still works for this session, just not remembered.
      }
      return next;
    });
  };

  return (
    <nav
      aria-label={t("sidebarLabel")}
      className={cn(
        "glass-surface flex shrink-0 flex-col gap-3 overflow-x-hidden overflow-y-auto border-r border-[var(--color-border)] p-3 transition-[width]",
        effectiveCollapsed ? "w-[72px]" : "w-56",
      )}
    >
      <div className={cn("flex items-center gap-2 px-1 pb-1", effectiveCollapsed && "justify-center")}>
        <span
          aria-hidden="true"
          className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md text-xs font-extrabold"
          style={{ background: "var(--admin-logo-bg)", color: "var(--admin-logo-fg)" }}
        >
          A
        </span>
        {!effectiveCollapsed && <span className="text-[15px] font-extrabold tracking-tight">AlgoPrep</span>}
      </div>

      <button
        type="button"
        onClick={toggleCollapsed}
        title={t("toggleSidebar")}
        aria-label={t("toggleSidebar")}
        className="flex h-8 items-center justify-center gap-2 rounded-md border border-[var(--color-border)] text-xs font-semibold text-[var(--color-text-muted)] hover:bg-[var(--color-surface-hover)]"
      >
        <span aria-hidden="true">{effectiveCollapsed ? "»" : "«"}</span>
        {!effectiveCollapsed && <span>{t("collapseSidebar")}</span>}
      </button>

      <div className="flex flex-1 flex-col gap-1 overflow-x-hidden overflow-y-auto">
        <NavLink
          href={ADMIN_NAV_OVERVIEW.href}
          label={t(ADMIN_NAV_OVERVIEW.labelKey)}
          icon={ADMIN_NAV_OVERVIEW.icon}
          isActive={pathname === ADMIN_NAV_OVERVIEW.href}
          collapsed={effectiveCollapsed}
        />

        {ADMIN_NAV_GROUPS.map((group) => {
          const isOpen = !!openGroups[group.key] && !effectiveCollapsed;
          return (
            <div key={group.key}>
              <NavGroupHeader
                label={t(group.labelKey)}
                icon={group.icon}
                isOpen={isOpen}
                collapsed={effectiveCollapsed}
                onClick={() => toggleGroup(group.key)}
              />
              {isOpen && (
                <div className="mt-1 ml-4 flex flex-col gap-0.5 border-l border-[var(--color-border)] pl-2">
                  {group.items.map((item) => (
                    <NavLink
                      key={item.key}
                      href={item.href}
                      label={t(item.labelKey)}
                      isActive={pathname === item.href}
                      collapsed={effectiveCollapsed}
                    />
                  ))}
                </div>
              )}
            </div>
          );
        })}

        <div className={cn("mt-3 border-t border-[var(--color-border)] pt-2", effectiveCollapsed && "text-center")}>
          {!effectiveCollapsed && (
            <p className="px-3 pb-1 text-xs font-semibold uppercase tracking-wide text-[var(--color-text-muted)]">
              {t("groupMisc")}
            </p>
          )}
          <div className="flex flex-col gap-0.5">
            {ADMIN_NAV_MISC.map((item) => (
              <NavLink
                key={item.key}
                href={item.href}
                label={t(item.labelKey)}
                icon={item.icon}
                isActive={pathname === item.href}
                collapsed={effectiveCollapsed}
              />
            ))}
          </div>
        </div>
      </div>

      <ThemeLangSwitcher variant="inline" collapsed={effectiveCollapsed} />
    </nav>
  );
}
