"use client";

import type { ReactNode } from "react";
import { ThemeProvider as NextThemesProvider } from "next-themes";

const VALID_THEMES = new Set(["light", "dark"]);

/**
 * Self-heals a corrupted `localStorage["theme"]` value BEFORE `NextThemesProvider` ever reads it.
 * A 2026-09-15 bug in `ThemeLangSwitcher` briefly called `setTheme` with the whole `{key, icon}`
 * option object instead of its string key while the sidebar/theme-toggle icon work was mid-edit
 * (caught by Fast Refresh recompiling the two-part change out of order in an already-open browser
 * tab) — that stored the literal string `"[object Object]"`, which next-themes then handed straight
 * to `classList.add()` on every subsequent load, crashing with `InvalidCharacterError` (the string
 * contains a space) before React could render anything. The bug in `theme-lang-switcher.tsx` is
 * already fixed, but anyone who hit it stays stuck on a permanently broken page without this: it
 * runs synchronously in the render body (not an effect) so it completes before
 * `NextThemesProvider` mounts and reads the same key.
 */
function clearCorruptedThemeStorage() {
  if (typeof window === "undefined") return;
  try {
    const stored = window.localStorage.getItem("theme");
    if (stored !== null && !VALID_THEMES.has(stored)) {
      window.localStorage.removeItem("theme");
    }
  } catch {
    // Private window / blocked storage — nothing to clean up, next-themes falls back to defaultTheme.
  }
}

// Light is the default, Dark is optional (DEC-2026-0824-dark-light-theme). attribute="class"
// matches .dark in globals.css.
export function ThemeProvider({ children }: { children: ReactNode }) {
  clearCorruptedThemeStorage();

  return (
    <NextThemesProvider attribute="class" defaultTheme="light" enableSystem={false}>
      {children}
    </NextThemesProvider>
  );
}
