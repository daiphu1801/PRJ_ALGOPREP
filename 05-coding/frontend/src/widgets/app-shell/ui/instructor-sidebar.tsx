// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useEffect, useState } from "react";
import { usePathname } from "next/navigation";
import { NavLink, ThemeLangSwitcher } from "@/shared/ui";
import { useT } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { INSTRUCTOR_NAV_MAIN, INSTRUCTOR_NAV_MISC } from "../model/instructor-nav";

/**
 * Layout follows 09-layoutBase/Giáo viên - Tổng quan.dc.html:63-121 — the same sidebar markup is
 * repeated verbatim by all 5 Instructor screens, so it belongs in the shell, not in any one view
 * (same reasoning as AdminSidebar). Top to bottom: logo + "GIÁO VIÊN" eyebrow (:64-72), collapse
 * toggle (:74-77), flat nav list (:79-90 — flat, NOT grouped like Admin), "KHÁC" group (:92-99),
 * theme switch (:101-107), user block (:109-119).
 *
 * Collapse state persists under its own `algoprep-teacher-collapsed` key, matching the mockup's
 * per-area `algoprep-teacher-theme` convention (:294) — an instructor collapsing this sidebar
 * should not also collapse the Admin one.
 */
export function InstructorSidebar() {
  const t = useT("instructorNav");
  const pathname = usePathname();

  // Same two-source rule as AdminSidebar: the persisted preference OR a narrow viewport forces the
  // icon rail. Without the viewport check the sidebar never narrows below `lg`.
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

  // Longest-prefix wins, not "every prefix match wins": a child route can sit under more than one
  // nav href, and a plain startsWith test lit up both at once.
  const activeHref = [...INSTRUCTOR_NAV_MAIN, ...INSTRUCTOR_NAV_MISC]
    .filter((item) => pathname === item.href || pathname.startsWith(`${item.href}/`))
    .sort((a, b) => b.href.length - a.href.length)[0]?.href;

  const toggleCollapsed = () => {
    setCollapsed((prev) => {
      const next = !prev;
      try {
        localStorage.setItem("algoprep-teacher-collapsed", next ? "1" : "0");
      } catch {
        // Private window / blocked storage — collapse still works, it just is not remembered.
      }
      return next;
    });
  };

  return (
    <nav
      aria-label={t("sidebarLabel")}
      className={cn(
        // sticky + h-screen, per dc.html:63 (`position: sticky; top: 0; height: 100vh`): without
        // it the sidebar is just a tall flex child, so a long table in <main> stretches the page
        // and scrolls the whole nav out of view. Reported by the owner 2026-09-27 — the student
        // list "kéo cả sidebar". `self-start` keeps sticky working inside the flex row (a stretched
        // item has no room to stick in).
        "glass-surface sticky top-0 flex h-screen shrink-0 flex-col gap-3 self-start overflow-x-hidden overflow-y-auto border-r border-[var(--color-border)] p-3 transition-[width]",
        effectiveCollapsed ? "w-[72px]" : "w-[244px]",
      )}
    >
      <div className={cn("flex items-center gap-2 px-1 pb-1", effectiveCollapsed && "justify-center")}>
        <span
          aria-hidden="true"
          className="flex h-[30px] w-[30px] shrink-0 items-center justify-center rounded-[9px] text-[13px] font-extrabold"
          style={{ background: "var(--instructor-logo-bg)", color: "var(--instructor-logo-fg)" }}
        >
          A
        </span>
        {!effectiveCollapsed && (
          <span className="min-w-0">
            <span className="block text-[15px] font-extrabold tracking-tight">AlgoPrep</span>
            {/* dc.html:69 — the eyebrow under the wordmark is what tells the two areas apart. */}
            <span className="block font-mono text-[9.5px] tracking-[0.16em] text-[var(--color-text-subtle)]">
              {t("areaEyebrow")}
            </span>
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

      <div className="flex flex-1 flex-col gap-0.5 overflow-x-hidden overflow-y-auto">
        {INSTRUCTOR_NAV_MAIN.map((item) => (
          <NavLink
            key={item.key}
            href={item.href}
            label={t(item.labelKey)}
            icon={item.icon}
            // Prefix match, not equality: /instructor/classes/<id>/students/<id> must still light
            // up "Lớp của tôi". "/instructor/overview" has no children so it is unaffected.
            isActive={item.href === activeHref}
            collapsed={effectiveCollapsed}
          />
        ))}

        <div className={cn("mt-3 border-t border-[var(--color-border)] pt-2", effectiveCollapsed && "text-center")}>
          {!effectiveCollapsed && (
            <p className="px-3 pb-1 text-xs font-semibold tracking-wide text-[var(--color-text-subtle)] uppercase">
              {t("groupMisc")}
            </p>
          )}
          <div className="flex flex-col gap-0.5">
            {INSTRUCTOR_NAV_MISC.map((item) => (
              <NavLink
                key={item.key}
                href={item.href}
                label={t(item.labelKey)}
                icon={item.icon}
                isActive={item.href === activeHref}
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
