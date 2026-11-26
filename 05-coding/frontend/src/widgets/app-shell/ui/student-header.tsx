// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { SHELL_PADDING_X, SHELL_PADDING_Y } from "@/shared/api/config";
import { BrandMark, Button, ThemeLangSwitcher } from "@/shared/ui";
import { useT } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { Menu, User, X } from "lucide-react";
import { UserAvatar, useMyProfile, useSession } from "@/entities/user";
import { LoginPromptDialog, useLoginPrompt } from "@/features/login-prompt";
import { JudgeEngineTag } from "./judge-engine-tag";
import { isGuestReadablePath } from "../model/guest-paths";
import { STUDENT_MENU_ITEMS, STUDENT_NAV_ITEMS } from "../model/student-nav";
import { LOGOUT_ITEM_KEY, useSignOut } from "../model/use-sign-out";

/**
 * Matches 09-layoutBase/Dashboard AlgoPrep.dc.html:50-92 (the same header every Student screen
 * repeats): a sticky, full-bleed strip whose inner bar is a full-width 58px glass pill —
 * brand + GO-JUDGE badge on the left, the nav in the middle, theme/language switcher and the
 * avatar menu on the right. Before this existed the whole Student area rendered the placeholder
 * header from `app-shell.tsx` (brand + area name only), so none of the 12 screens had any
 * navigation at all.
 *
 * Nav destinations and the two prototype items that were dropped: see `model/student-nav.ts`.
 *
 * Active state uses a prefix match, not equality, so /problems/two-sum keeps "Bài toán" marked —
 * `/` is excluded from the prefix test since no nav href is the root.
 */
export function StudentHeader() {
  const t = useT("nav");
  const pathname = usePathname();
  const [menuOpen, setMenuOpen] = useState(false);
  const signOut = useSignOut();
  // Khách (DEC-2026-1126): thấy đủ mục nav, bấm mục cần phiên thì hiện hộp thoại nhắc đăng nhập;
  // khối người dùng thành nút Đăng nhập, không gọi hồ sơ.
  const guest = useSession() === "guest";
  const prompt = useLoginPrompt();
  const profile = useMyProfile("student").data;
  // Separate from `menuOpen` (the avatar dropdown): this is the narrow-viewport nav drawer that
  // 02-bd/screens/users/_shell.md mục 6 Q5 specifies — below the breakpoint the nav items collapse
  // behind one button while the brand and the user block stay put.
  const [navOpen, setNavOpen] = useState(false);

  const navButtonRef = useRef<HTMLButtonElement>(null);
  const menuButtonRef = useRef<HTMLButtonElement>(null);

  // Escape closes whichever popup is open and hands focus back to the button that opened it.
  // Listens on the document (only while a popup is open) so the non-interactive <header> carries
  // no key handler.
  useEffect(() => {
    if (!menuOpen && !navOpen) return;
    function onKeyDown(event: KeyboardEvent) {
      if (event.key !== "Escape") return;
      if (menuOpen) {
        setMenuOpen(false);
        menuButtonRef.current?.focus();
      } else {
        setNavOpen(false);
        navButtonRef.current?.focus();
      }
    }
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [menuOpen, navOpen]);

  const isActive = (href: string) =>
    pathname === href || pathname.startsWith(`${href}/`);
  // Chỉ "Bài toán" mở cho khách; sáu mục còn lại cần phiên.
  const needsSession = (href: string) => guest && !isGuestReadablePath(href);

  return (
    <header
      className={cn(
        "sticky top-0 z-30 bg-[linear-gradient(var(--color-background)_82%,transparent)]",
        SHELL_PADDING_X,
        SHELL_PADDING_Y,
        "pb-0",
      )}
    >
      <div className="glass-surface relative flex h-[58px] w-full items-center gap-2 rounded-2xl border border-[var(--color-border)] px-3 shadow-sm sm:gap-4 sm:px-4 lg:gap-7 lg:px-5">
        <button
          ref={navButtonRef}
          type="button"
          onClick={() => setNavOpen((open) => !open)}
          aria-expanded={navOpen}
          aria-label={t("toggleNav")}
          className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-[var(--color-border)] text-[var(--color-text-muted)] hover:text-[var(--color-text)] lg:hidden"
        >
          {navOpen ? <X className="h-4 w-4" /> : <Menu className="h-4 w-4" />}
        </button>

        <Link
          href="/dashboard"
          className="flex shrink-0 items-center gap-2.5 text-[var(--color-text)]"
        >
          <BrandMark size={32} />
          {/* A guest's login button is wider than the avatar: below sm the wordmark gives it the room
              (the logo stays, screen readers still hear the name). */}
          <span
            className={cn(
              "text-[19px] font-bold tracking-tight",
              guest && "sr-only sm:not-sr-only",
            )}
          >
            AlgoPrep
          </span>
          {/* Hidden below sm: brand + tag + the right cluster do not fit a 390px bar. */}
          <span className="hidden sm:contents">
            <JudgeEngineTag />
          </span>
        </Link>

        <nav
          aria-label={t("area.student")}
          className="hidden min-w-0 flex-1 items-center gap-5 lg:flex"
        >
          {STUDENT_NAV_ITEMS.map((item) => {
            const className = cn(
              "whitespace-nowrap border-b-2 pb-0.5 text-[14.5px] transition-colors",
              isActive(item.href)
                ? "border-[var(--color-primary)] font-semibold text-[var(--color-text)]"
                : "border-transparent font-medium text-[var(--color-text-muted)] hover:text-[var(--color-text)]",
            );
            return needsSession(item.href) ? (
              <button
                key={item.key}
                type="button"
                className={className}
                onClick={() => prompt.ask(item.href)}
              >
                {t(item.labelKey)}
              </button>
            ) : (
              <Link
                key={item.key}
                href={item.href}
                aria-current={isActive(item.href) ? "page" : undefined}
                className={className}
              >
                {t(item.labelKey)}
              </Link>
            );
          })}
        </nav>

        {/* Keeps the user block hard right once the inline nav is hidden — without it the two
            remaining children sit side by side against the brand. */}
        <div className="flex-1 lg:hidden" />

        <div className="flex shrink-0 items-center gap-1.5 sm:gap-3">
          <ThemeLangSwitcher iconOnlyBelowSm />

          {guest ? (
            <Button asChild variant="cta" size="sm">
              <Link href={`/login?redirect=${encodeURIComponent(pathname)}`}>
                {t("login")}
              </Link>
            </Button>
          ) : (
            <div className="relative">
              <button
                ref={menuButtonRef}
                type="button"
                onClick={() => setMenuOpen((open) => !open)}
                aria-expanded={menuOpen}
                aria-label={t("accountMenu")}
                className="flex h-8 w-8 items-center justify-center overflow-hidden rounded-full border border-[var(--color-border)] bg-[var(--color-surface-hover)] text-[11.5px] font-semibold text-[var(--color-text-muted)] hover:text-[var(--color-text)]"
              >
                {/* The signed-in learner's picture or initials once the profile has loaded; a generic
                  glyph until then, rather than inventing a person's name. */}
                {profile ? (
                  <UserAvatar
                    userId={profile.userId}
                    name={profile.displayName}
                    avatarVersion={profile.avatarVersion}
                    size="sm"
                    className="border-0"
                  />
                ) : (
                  <User aria-hidden="true" className="h-4 w-4" />
                )}
              </button>

              {menuOpen && (
                <>
                  {/* Click-anywhere-to-close scrim, 1:1 with dc.html:78. Out of the tab order: Escape and the
                    opener button already close the menu for keyboard users. */}
                  <button
                    type="button"
                    tabIndex={-1}
                    aria-label={t("closeMenu")}
                    onClick={() => setMenuOpen(false)}
                    className="fixed inset-0 z-40 cursor-default"
                  />
                  {/* Plain links, not role="menu": arrow-key navigation is not implemented, so the menu role
                    would promise more than the keyboard gets. */}
                  <div className="absolute right-0 top-10 z-50 w-[258px] rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-2.5 shadow-lg">
                    {STUDENT_MENU_ITEMS.map((item, index) => (
                      <div key={item.key}>
                        {item.danger && (
                          <div className="-mx-2.5 my-2 h-px bg-[var(--color-border)]" />
                        )}
                        <Link
                          href={item.href}
                          onClick={(event) => {
                            setMenuOpen(false);
                            if (item.key === LOGOUT_ITEM_KEY) {
                              event.preventDefault();
                              void signOut(item.href);
                            }
                          }}
                          className={cn(
                            "flex h-[34px] items-center gap-2.5 rounded-lg px-2.5 text-[13.5px] font-medium hover:bg-[var(--color-surface-hover)]",
                            item.danger
                              ? "text-[var(--color-danger)]"
                              : "text-[var(--color-text)]",
                          )}
                          autoFocus={index === 0}
                        >
                          <item.icon
                            aria-hidden="true"
                            className="h-4 w-4 shrink-0"
                          />
                          {t(item.labelKey)}
                        </Link>
                      </div>
                    ))}
                  </div>
                </>
              )}
            </div>
          )}
        </div>

        {navOpen && (
          // Anchored to the header bar rather than being a full-screen overlay: there are only 6
          // destinations, so a sheet that covers the page would be more ceremony than the content
          // warrants. Hidden from `lg` up, where the inline nav is back.
          <nav
            aria-label={t("area.student")}
            // Opaque, not `glass-surface`: a translucent panel over a dense dashboard leaves the
            // page text legible straight through the menu.
            className="absolute left-0 right-0 top-full z-40 mt-2 flex flex-col rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-2 shadow-lg lg:hidden"
          >
            {STUDENT_NAV_ITEMS.map((item) => {
              const className = cn(
                "rounded-lg px-3 py-2 text-left text-[14.5px]",
                isActive(item.href)
                  ? "bg-[var(--color-surface-hover)] font-semibold text-[var(--color-text)]"
                  : "font-medium text-[var(--color-text-muted)] hover:bg-[var(--color-surface-hover)]",
              );
              return needsSession(item.href) ? (
                <button
                  key={item.key}
                  type="button"
                  className={className}
                  onClick={() => {
                    setNavOpen(false);
                    prompt.ask(item.href);
                  }}
                >
                  {t(item.labelKey)}
                </button>
              ) : (
                <Link
                  key={item.key}
                  href={item.href}
                  onClick={() => setNavOpen(false)}
                  aria-current={isActive(item.href) ? "page" : undefined}
                  className={className}
                >
                  {t(item.labelKey)}
                </Link>
              );
            })}
          </nav>
        )}
      </div>
      <LoginPromptDialog
        open={prompt.open}
        returnTo={prompt.returnTo}
        onClose={prompt.close}
      />
    </header>
  );
}
