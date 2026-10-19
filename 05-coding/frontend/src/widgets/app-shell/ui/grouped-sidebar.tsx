// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useEffect, useState } from "react";
import { usePathname } from "next/navigation";
import type { LucideIcon } from "lucide-react";
import {
  BrandMark,
  NavGroupHeader,
  NavLink,
  ThemeLangSwitcher,
} from "@/shared/ui";
import { useT } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import type { AdminNavGroup, AdminNavItem } from "../model/admin-nav";

type Props = {
  /** Message namespace holding the nav labels (`adminNav`, `instructorNav`). */
  namespace: "adminNav" | "instructorNav";
  /** Ungrouped first link; the Instructor area has none since its overview screen was dropped. */
  overview?: AdminNavItem & { icon: LucideIcon };
  groups: AdminNavGroup[];
  /** localStorage key of the persisted collapse preference, one per area. */
  collapsedStorageKey: string;
  /** Optional small line under the wordmark that names the area (instructor shows "GIÁO VIÊN"). */
  eyebrowKey?: string;
  /**
   * Admin matches a route exactly (its detail pages are not in the nav); the instructor area has
   * nested routes (/instructor/classes/<id>/students/<id>), so it lights the longest href prefix.
   */
  matchPrefix?: boolean;
  /** Groups start open instead of only the one owning the route (instructor, owner 2026-10-13: Q-K). */
  defaultOpenAll?: boolean;
  /**
   * A detail page can keep the item of the screen that opened it lit (instructor result page, `from=`),
   * which the pathname alone cannot tell. When given, it wins over the pathname match.
   */
  activeHrefOverride?: string;
};

/**
 * Layout matches 09-layoutBase/Admin - Tổng quan.dc.html:57-101 1:1 (02-bd/screens/admin/ADM0101_overview.md
 * section 2 point 1): logo + collapse toggle live INSIDE the sidebar (not the toolbar), and the
 * theme switcher sits at the sidebar's footer. Clicking a group header only expands/collapses it —
 * it does NOT navigate (BD section 6, last bullet).
 *
 * Extracted from AdminSidebar 2026-10-11 so the Instructor area has the same layout (owner
 * instruction: the instructor UI follows the admin layout). The Admin behaviour is unchanged.
 *
 * Collapse state persists like the prototype's `algoprep-admin-collapsed` localStorage key (dc.html
 * line 308), read via `localStorage` guarded in try/catch — a per-viewer UI convenience, not app
 * state, so localStorage is an acceptable prototype shortcut here.
 */
export function GroupedSidebar({
  namespace,
  overview,
  groups,
  collapsedStorageKey,
  eyebrowKey,
  matchPrefix = false,
  defaultOpenAll = false,
  activeHrefOverride,
}: Props) {
  const t = useT(namespace);
  const pathname = usePathname();
  // Two independent sources decide the visual state, not one: `collapsed` is the user's own
  // persisted preference (toggle button, localStorage); `isNarrowViewport` is a live viewport
  // check (below `lg`, 1024px). A narrow viewport always forces the icon-rail width regardless of
  // the saved preference (owner flagged the sidebar as "bị dãn quá" on 2026-09-14).
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

  // Restore the saved preference after mount (reading it during render would break hydration).
  // Until 2026-10-14 the key was only ever written, so a reload always re-expanded the sidebar
  // (admin UX review F7).
  useEffect(() => {
    const restoreSaved = () => {
      try {
        setCollapsed(localStorage.getItem(collapsedStorageKey) === "1");
      } catch {
        // Blocked storage: keep the expanded default.
      }
    };
    restoreSaved();
  }, [collapsedStorageKey]);

  // The href that owns the current route: exact for Admin, longest prefix for nested routes.
  const allHrefs = [
    ...(overview ? [overview.href] : []),
    ...groups.flatMap((g) => g.items.map((i) => i.href)),
  ];
  const activeHref =
    activeHrefOverride ??
    (matchPrefix
      ? allHrefs
          .filter(
            (href) => pathname === href || pathname.startsWith(`${href}/`),
          )
          .sort((a, b) => b.length - a.length)[0]
      : allHrefs.find((href) => href === pathname));

  // Independent per-group open state — deliberately NOT a single-open accordion (owner 2026-09-14).
  // Holds ONLY groups the user toggled by hand; a group with no entry falls back to the
  // route-derived default below.
  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>({});

  // The group owning the current route defaults to open, so the sidebar shows where you are.
  const ownsActiveRoute = (key: string) =>
    defaultOpenAll ||
    (groups
      .find((group) => group.key === key)
      ?.items.some((item) => item.href === activeHref) ??
      false);
  const isGroupOpen = (key: string) => openGroups[key] ?? ownsActiveRoute(key);

  const toggleGroup = (key: string) =>
    setOpenGroups((prev) => ({
      ...prev,
      [key]: !(prev[key] ?? ownsActiveRoute(key)),
    }));
  const toggleCollapsed = () => {
    setCollapsed((prev) => {
      const next = !prev;
      try {
        localStorage.setItem(collapsedStorageKey, next ? "1" : "0");
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
        // Pinned to the viewport: without sticky + h-screen the nav is a tall flex child and scrolls
        // away with a long table. `self-start` lets sticky work in the row.
        "glass-surface sticky top-0 flex h-screen shrink-0 flex-col gap-3 self-start overflow-hidden border-r border-[var(--color-border)] p-3 transition-[width]",
        effectiveCollapsed ? "w-[72px]" : "w-56",
      )}
    >
      <div
        className={cn(
          "flex items-center gap-2 px-1 pb-1",
          effectiveCollapsed && "justify-center",
        )}
      >
        <BrandMark size={32} />
        {!effectiveCollapsed && (
          <span className="min-w-0">
            <span className="block text-[15px] font-extrabold tracking-tight">
              AlgoPrep
            </span>
            {eyebrowKey ? (
              <span className="block font-mono text-[9.5px] tracking-[0.16em] text-[var(--color-text-subtle)]">
                {t(eyebrowKey)}
              </span>
            ) : null}
          </span>
        )}
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

      {/* min-h-0 lets this list shrink and scroll on its own, so the logo, collapse button and theme switcher stay pinned. */}
      <div className="scrollbar-glass -mr-2 flex min-h-0 flex-1 flex-col gap-1 overflow-x-hidden overflow-y-auto pr-2">
        {overview ? (
          <NavLink
            href={overview.href}
            label={t(overview.labelKey)}
            icon={overview.icon}
            isActive={overview.href === activeHref}
            collapsed={effectiveCollapsed}
          />
        ) : null}

        {groups.map((group) => {
          const isOpen = isGroupOpen(group.key) && !effectiveCollapsed;
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
                      isActive={item.href === activeHref}
                      collapsed={effectiveCollapsed}
                    />
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </div>

      <ThemeLangSwitcher variant="inline" collapsed={effectiveCollapsed} />
    </nav>
  );
}
