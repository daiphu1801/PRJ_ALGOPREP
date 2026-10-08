// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import type { ReactNode } from "react";
import { cn } from "@/shared/lib";

type AuthAsidePanelProps = {
  /** Small pills across the top. Keep them to three — the column is 1fr of a max-w-3xl card. */
  badges: string[];
  title: string;
  description: string;
  /** Optional CTA under the copy — the student screen's login/signup switch. */
  action?: ReactNode;
  /** Illustration slot at the bottom: a code sample, a feature list, whatever the actor needs. */
  children?: ReactNode;
  className?: string;
};

/**
 * The filled right column of an auth screen (`02-bd/screens/shared/SHR0101_auth.md` Sheet 5, khu vực B).
 * Promoted out of `views/shared/auth` on 2026-09-29: the Instructor and Admin login screens were a
 * lone card on an empty page, and FSD forbids one views slice importing another, so the panel had
 * to move down a layer to be shared. It takes only presentation props — no AuthMode, no domain
 * type — which is what puts it in `shared/ui` per
 * `01-rd/system/SYS0102_frontend_architecture.md` section 2.A, same reasoning as `NavLink`.
 *
 * Colors come from `--color-primary`/`--color-on-primary`, so the panel picks up each actor's
 * palette from whichever shell scope wraps it (globals.css) with no per-actor variant: blue for
 * students, blue-grey inside `.instructor-shell`, cyan inside `.admin-shell`.
 */
export function AuthAsidePanel({
  badges,
  title,
  description,
  action,
  children,
  className,
}: AuthAsidePanelProps) {
  return (
    // Badges and dots are pinned to the panel's top and bottom while the copy and the illustration
    // stay ONE centred block, which is how the mockup lays the column out
    // (09-layoutBase/Đăng nhập & Đăng ký.dc.html:150 — badges `position: absolute; top: 32px`,
    // content `justify-content: center`). Flowing all four in a column instead — whether spread with
    // `justify-between` or pushed apart with `mt-auto` — opens a void in the middle whenever the
    // copy is short, which is exactly what the owner saw on the Admin screen (2026-09-29).
    // `py-20` reserves the band the two pinned rows sit in, so the centred block never runs under them.
    <aside
      className={cn(
        "relative hidden flex-col justify-center bg-[var(--color-primary)] px-8 py-20 text-[var(--color-on-primary)] lg:flex",
        className,
      )}
    >
      <div className="absolute inset-x-8 top-8 flex flex-wrap gap-2 text-xs font-medium">
        {badges.map((badge) => (
          <span key={badge} className="on-accent-tint rounded-full px-3 py-1">
            {badge}
          </span>
        ))}
      </div>

      <div>
        <h2 className="text-xl font-semibold">{title}</h2>
        <p className="mt-2 text-sm opacity-80">{description}</p>
        {action && <div className="mt-4">{action}</div>}
        {children && <div className="mt-6">{children}</div>}
      </div>

      <div className="absolute bottom-8 left-8 flex gap-1.5" aria-hidden="true">
        {[0, 1, 2].map((dot) => (
          <span
            key={dot}
            className="h-1.5 w-1.5 rounded-full bg-current"
            style={{ opacity: dot === 0 ? 1 : 0.4 }}
          />
        ))}
      </div>
    </aside>
  );
}

/**
 * The bottom slot's default shape for screens with no code sample to show: three labelled rows
 * naming what the actor actually does after logging in. Every label is a real nav destination
 * (`adminNav`/`instructorNav` in messages/), not invented marketing copy.
 */
export function AuthAsideHighlights({ items }: { items: string[] }) {
  return (
    <ul className="on-accent-tint space-y-2 rounded-lg p-4 text-[13px]">
      {items.map((item) => (
        <li key={item} className="flex items-center gap-2.5">
          <span
            aria-hidden="true"
            className="h-1.5 w-1.5 shrink-0 rounded-full bg-current opacity-70"
          />
          <span className="opacity-90">{item}</span>
        </li>
      ))}
    </ul>
  );
}
