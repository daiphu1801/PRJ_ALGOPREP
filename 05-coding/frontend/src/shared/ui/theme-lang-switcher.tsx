// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useEffect, useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { useTheme } from "next-themes";
import { Moon, Sun } from "lucide-react";
import { cn } from "@/shared/lib";
import { locales, setLocale, useLocale, useT } from "@/shared/i18n";

type ThemeLangSwitcherProps = {
  /** Fixed to a screen corner (auth screen) vs. inline in a toolbar/sidebar (admin shell). */
  variant?: "floating" | "inline";
  /**
   * Icon-only rail mode (the Admin sidebar collapsed to 72px). dc.html hides the theme tabs'
   * `<span>` label entirely at this width (line 65: `display: {{ expandedDisplay }}`) rather than
   * letting the full "Sáng"/"Tối" text clip against the narrower track — that clipping is exactly
   * what happened before this prop existed (owner-flagged 2026-09-15 screenshot of the collapsed
   * rail). The language group has no icon to fall back to, so it keeps its two-letter labels and
   * only loses the theme group's text.
   */
  collapsed?: boolean;
  className?: string;
};

// dc.html:38-39 (`t.icon` per theme tab) — icon + label in that order, gap 6px, icon 13px square.
const THEME_OPTIONS = [
  { key: "light", icon: Sun },
  { key: "dark", icon: Moon },
] as const;

/**
 * Theme (Light/Dark, DEC-2026-0824-dark-light-theme) + language (vi/en, DEC-2026-0824-i18n-vi-en)
 * switcher. Domain-agnostic — takes no business type — so it lives in shared/ui per
 * 01-rd/system/frontend_architecture.md section 2.A-B, and is reused by both the `auth` screen
 * (fixed corner widget, 02-bd/screens/shared/auth.md section 1) and the Admin shell sidebar
 * (02-bd/screens/admin/admin_overview.md section 2 point 6).
 *
 * Two DIFFERENT shapes on purpose, not one shared track twice: Theme stays a segmented pill —
 * 1:1 with the prototype's toggle group, BOTH options always visible inside one track
 * (09-layoutBase/Admin - Tổng quan.dc.html:96-100, 09-layoutBase/Đăng nhập & Đăng ký.dc.html:80-87).
 * Language is two independent circle buttons (matches the decorative icon buttons already in
 * `AdminToolbar`) — narrower than a second pill, which is what let the Admin sidebar's 224px width
 * fit both groups on one row again after 2026-09-16 (a rectangular language pill next to the theme
 * pill was the thing forcing them onto two stacked rows in the first place).
 */
export function ThemeLangSwitcher({ variant = "inline", collapsed = false, className }: ThemeLangSwitcherProps) {
  const t = useT("common");
  const locale = useLocale();
  const { setTheme } = useTheme();
  const router = useRouter();
  // `startTransition` marks the RSC refresh as low-priority: the language buttons update their
  // own active/pressed state and next-themes' class toggle applies instantly, the (slower)
  // server-rendered text streams in without blocking that first paint or freezing the click.
  const [, startTransition] = useTransition();
  const handleLocaleChange = (nextLocale: (typeof locales)[number]) => {
    setLocale(nextLocale);
    startTransition(() => {
      router.refresh();
    });
  };
  // Reads `<html class>` directly instead of trusting `useTheme()`'s `resolvedTheme` for display.
  // Empirically (Playwright, 2026-09-16) `resolvedTheme` can get stuck reporting "light" after a
  // full page reload/`window.location.reload()` (the language switcher's own reload included) even
  // though `<html class="dark">` is correctly applied and clicking a theme button still works fine
  // — the desync is specifically in the VALUE the hook reports on that first render, not in
  // next-themes' own DOM management. `<html class>` is next-themes' actual source of truth (its own
  // blocking pre-hydration script sets it directly from localStorage before our component ever
  // mounts), so reading it ourselves sidesteps whatever that hook-level race is instead of
  // depending on a value observed to disagree with it. A `MutationObserver` keeps this in sync with
  // any later change (this button's own click, the OS theme, another tab via next-themes' `storage`
  // listener) without polling.
  const [activeTheme, setActiveTheme] = useState<"light" | "dark">("light");
  useEffect(() => {
    const root = document.documentElement;
    const sync = () => setActiveTheme(root.classList.contains("dark") ? "dark" : "light");
    sync();
    const observer = new MutationObserver(sync);
    observer.observe(root, { attributes: true, attributeFilter: ["class"] });
    return () => observer.disconnect();
  }, []);
  // Only the collapsed 72px rail forces the two groups onto separate rows now. The 224px expanded
  // sidebar fits both in ONE row as long as the language group is two small circles (`h-7 w-7`,
  // ~60px total) instead of a second full pill (~74px) — that's the width this was overflowing by
  // before. Circles also read as a lighter-weight control than a second pill duplicating the theme
  // track's shape, which is why the owner asked for them specifically (2026-09-16), not just "make
  // it narrower". `variant="floating"` (the auth screen's corner widget) always had room to spare.
  const stacked = collapsed;

  // "nổi lên" (owner 2026-09-16): in Light theme `--color-primary` inside `.admin-shell` is a
  // near-white `rgba(255,255,255,0.85)` sitting on an almost-as-light glass track — a flat color
  // swap alone barely reads as "selected" there (still true in Dark, just less needed since
  // `--color-primary` is a saturated cyan). A small multi-layer shadow gives the active segment its
  // own contact shadow, like a raised tab/chip floating a hair above the track, instead of relying
  // on background contrast alone.
  const activeElevation =
    "shadow-[0_1px_2px_rgba(6,40,36,0.16),0_3px_6px_rgba(6,40,36,0.14)] dark:shadow-[0_1px_2px_rgba(0,0,0,0.4),0_3px_8px_rgba(0,0,0,0.35)]";

  // Whole-row budget, not just this pill's own look: the Admin sidebar is only 224px wide with
  // `p-3` (12px) on both sides (`admin-sidebar.tsx`), so this row has exactly 200px to spend on
  // BOTH groups + the gap between them. The locale-stable `min-w` fix below already commits both
  // theme buttons to their widest possible content ("Light"/"Dark"), which alone is ~12px more
  // than the old text-driven width — that overflow is what put the language circles flush against
  // the sidebar's `border-r` with zero margin (owner screenshot, 2026-09-16: "sát vào thanh border
  // ở sidebar"). Padding the pill generously (an earlier pass here) made it worse by spending more
  // of that same 200px. Fixed by trimming padding/gaps back down AND shrinking the language
  // circles by 4px each — verified live (Playwright bounding boxes) to land ~20px clear of the
  // border, not just "should fit" by eye.
  const segmentedTrack = cn(
    "glass-surface flex h-8 gap-0.5 rounded-lg border border-[var(--color-border)] px-1 py-0.5",
    stacked && "w-full",
  );
  const segmentButton = (active: boolean) =>
    cn(
      "flex h-full items-center justify-center gap-1.5 rounded-md text-xs font-semibold transition-colors",
      // Fixed min-width instead of letting the label size the button: "Sáng"/"Tối" and
      // "Light"/"Dark" are NOT the same pixel width, so switching locale was changing the track's
      // total width and shifting whatever sits next to it (owner-flagged 2026-09-16 screenshot: the
      // VI/EN circles visibly jump right on switching to English). 62px fits the widest of the four
      // labels ("Light") with `px-1.5` — see the sidebar-budget note above for why not wider.
      stacked ? "flex-1 px-1.5" : "min-w-[62px] px-1.5",
      active
        ? cn("bg-[var(--color-primary)] text-[var(--color-on-primary)]", activeElevation)
        : "text-[var(--color-text-muted)] hover:bg-[var(--color-surface-hover)] hover:text-[var(--color-text)]",
    );
  const circleButton = (active: boolean) =>
    cn(
      "glass-surface flex shrink-0 items-center justify-center rounded-full border border-[var(--color-border)] font-bold uppercase transition-colors",
      // Collapsed rail is a SEPARATE width budget from the expanded sidebar and was never
      // re-measured after the 2026-09-16 padding pass: 72px rail with p-3 leaves 48px of content,
      // while two 24px circles plus gap-1 need 52px. Measured 2026-09-17 (Playwright bounding box):
      // the group started 9.5px from the rail edge instead of 12px, eating 2.5px of the gutter on
      // each side. 20px circles with a 2px gap need 42px and leave real slack.
      stacked ? "h-5 w-5 text-[10px]" : "h-6 w-6 text-[10.5px]",
      active
        ? cn("bg-[var(--color-primary)] text-[var(--color-on-primary)]", activeElevation)
        : "text-[var(--color-text-muted)] hover:bg-[var(--color-surface-hover)] hover:text-[var(--color-text)]",
    );

  return (
    <div
      className={cn(
        "flex items-center gap-1",
        variant === "floating" && "fixed right-4 top-4 z-50",
        stacked && "flex-col",
        className,
      )}
    >
      <div role="group" aria-label={t("themeSwitcher")} className={segmentedTrack}>
        {THEME_OPTIONS.map(({ key: option, icon: Icon }) => (
          // suppressHydrationWarning: `activeTheme` is a plain useState starting at "light" (SSR
          // and first client paint agree, so there's no real mismatch to suppress) — kept as a
          // guard for the one render tick between hydration and the sync effect above resolving the
          // true class, same as app/layout.tsx does for next-themes' own <html> attribute.
          <button
            key={option}
            type="button"
            onClick={() => setTheme(option)}
            title={collapsed ? (option === "dark" ? t("themeDark") : t("themeLight")) : undefined}
            aria-pressed={activeTheme === option}
            className={segmentButton(activeTheme === option)}
            suppressHydrationWarning
          >
            <Icon aria-hidden="true" className="h-3.5 w-3.5" />
            {!collapsed && (option === "dark" ? t("themeDark") : t("themeLight"))}
          </button>
        ))}
      </div>
      <div
        role="group"
        aria-label={t("languageSwitcher")}
        className={cn("flex items-center", stacked ? "gap-0.5" : "gap-1")}
      >
        {locales.map((candidate) => (
          <button
            key={candidate}
            type="button"
            onClick={() => handleLocaleChange(candidate)}
            title={t("languageSwitcher")}
            aria-pressed={locale === candidate}
            className={circleButton(locale === candidate)}
          >
            {candidate}
          </button>
        ))}
      </div>
    </div>
  );
}
