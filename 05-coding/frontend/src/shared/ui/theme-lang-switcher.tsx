// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useTheme } from "next-themes";
import { cn } from "@/shared/lib";
import { locales, setLocale, useLocale, useT } from "@/shared/i18n";

type ThemeLangSwitcherProps = {
  /** Fixed to a screen corner (auth screen) vs. inline in a toolbar/sidebar (admin shell). */
  variant?: "floating" | "inline";
  className?: string;
};

/**
 * Theme (Light/Dark, DEC-2026-0824-dark-light-theme) + language (vi/en, DEC-2026-0824-i18n-vi-en)
 * switcher. Domain-agnostic — takes no business type — so it lives in shared/ui per
 * 01-rd/system/frontend_architecture.md section 2.A-B, and is reused by both the `auth` screen
 * (fixed corner widget, 02-bd/screens/shared/auth.md section 1) and the Admin shell sidebar
 * (02-bd/screens/admin/admin_overview.md section 2 point 6).
 */
export function ThemeLangSwitcher({ variant = "inline", className }: ThemeLangSwitcherProps) {
  const t = useT("common");
  const locale = useLocale();
  const { resolvedTheme, setTheme } = useTheme();
  const isDark = resolvedTheme === "dark";

  return (
    <div
      className={cn(
        "flex items-center gap-2",
        variant === "floating" && "fixed right-4 top-4 z-50",
        className,
      )}
    >
      <button
        type="button"
        onClick={() => setTheme(isDark ? "light" : "dark")}
        className="glass-surface h-8 rounded-md border border-[var(--color-border)] px-3 text-xs font-medium text-[var(--color-text)]"
        aria-label={t(isDark ? "themeSwitchToLight" : "themeSwitchToDark")}
      >
        {isDark ? t("themeDark") : t("themeLight")}
      </button>
      <div
        role="group"
        aria-label={t("languageSwitcher")}
        className="glass-surface flex h-8 overflow-hidden rounded-md border border-[var(--color-border)] text-xs font-medium"
      >
        {locales.map((candidate) => (
          <button
            key={candidate}
            type="button"
            onClick={() => setLocale(candidate)}
            aria-pressed={locale === candidate}
            className={cn(
              "px-2.5 uppercase",
              locale === candidate
                ? "bg-[var(--color-primary)] text-[var(--color-on-primary)]"
                : "text-[var(--color-text)] hover:bg-[var(--color-surface-hover)]",
            )}
          >
            {candidate}
          </button>
        ))}
      </div>
    </div>
  );
}
