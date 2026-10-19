// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { ChevronDown, type LucideIcon } from "lucide-react";
import { cn } from "@/shared/lib";

/**
 * The user block of the area toolbar (Admin and Instructor share it, 2026-10-11) as a pop-up menu. The "KHÁC" links (settings, auth, profile)
 * used to sit at the foot of the sidebar; they live here now so the sidebar is shorter (owner
 * 2026-10-10). The theme/language switcher stays in the sidebar footer by owner request.
 * Closes on outside press, Escape and picking a link.
 */
export type AreaMenuItem = {
  key: string;
  href: string;
  label: string;
  icon: LucideIcon;
};

export function AreaUserMenu({
  name,
  roleLabel,
  items,
}: {
  name: string;
  roleLabel: string;
  items: AreaMenuItem[];
}) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onPointerDown = (e: PointerEvent) => {
      if (!rootRef.current?.contains(e.target as Node)) setOpen(false);
    };
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  return (
    <div ref={rootRef} className="relative ml-2">
      <button
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
        className="flex items-center gap-2 rounded-full py-0.5 pr-2 text-left hover:bg-[var(--color-surface-hover)]"
      >
        <span
          className="flex h-8 w-8 items-center justify-center rounded-full bg-[image:var(--color-cta-bg)] text-xs font-semibold text-[var(--color-cta-fg)]"
          aria-hidden="true"
        >
          {name.charAt(0).toUpperCase() || "A"}
        </span>
        <span className="leading-tight">
          <span className="block text-[12.5px] font-bold text-[var(--color-text)]">
            {name}
          </span>
          <span className="block text-[11px] text-[var(--color-text-muted)]">
            {roleLabel}
          </span>
        </span>
        <ChevronDown
          aria-hidden="true"
          className={cn(
            "h-4 w-4 text-[var(--color-text-muted)] transition-transform",
            open && "rotate-180",
          )}
        />
      </button>

      {open && (
        <div
          role="menu"
          className="glass-card glass-card--popover absolute right-0 top-full z-50 mt-2 flex w-60 flex-col gap-1 p-2"
        >
          {items.map(({ key, href, label, icon: Icon }) => (
            <Link
              key={key}
              role="menuitem"
              href={href}
              onClick={() => setOpen(false)}
              className="flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium text-[var(--color-text)] hover:bg-[var(--color-surface-hover)]"
            >
              <Icon aria-hidden="true" className="h-4 w-4" />
              {label}
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
