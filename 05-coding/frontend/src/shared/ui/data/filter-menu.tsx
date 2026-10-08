// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// List filter as a button that opens a small glass pop-up of options, instead of a row of pills
// (owner instruction 2026-10-05). Same props as SegmentedTabs so a screen swaps one for the other.
// It scales to option lists an admin can grow without limit, which a row of pills cannot.
//
// The panel is rendered in place (absolutely positioned under the trigger), NOT portalled to <body>:
// the Admin and Instructor shells set their glass/border tokens on a wrapper element, and a portal
// would escape that scope and pick up the app-wide defaults. The panel uses `glass-card--popover`, the
// denser sibling of the dialog glass, so it reads as the same family as the "Quản lý ..." dialogs.
//
// Semantics: the trigger is a button with aria-haspopup="listbox"; the panel is a listbox of options
// (aria-selected). Arrow keys / Home / End move between options, Enter or Space picks, Escape closes and
// returns focus to the trigger, Tab or a click outside just closes.
"use client";

import { useEffect, useId, useRef, useState, type KeyboardEvent } from "react";
import { Check, ChevronDown } from "lucide-react";
import { cn } from "@/shared/lib";

type FilterMenuProps<T extends string> = {
  /** Accessible name and visible prefix of the trigger, e.g. "Lọc theo độ khó". */
  label: string;
  options: { value: T; label: string }[];
  value: T;
  onValueChange: (value: T) => void;
  /** Value-only trigger (the label stays as the accessible name), for small pickers such as rows per page. */
  compact?: boolean;
  /** Which way the panel opens; "top" for a control at the bottom of a card, such as the pagination bar. */
  placement?: "bottom" | "top";
  className?: string;
};

export function FilterMenu<T extends string>({
  label,
  options,
  value,
  onValueChange,
  compact = false,
  placement = "bottom",
  className,
}: FilterMenuProps<T>) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const listId = useId();
  const current = options.find((option) => option.value === value);

  useEffect(() => {
    if (!open) return;
    const items =
      listRef.current?.querySelectorAll<HTMLElement>('[role="option"]');
    const selected = listRef.current?.querySelector<HTMLElement>(
      '[aria-selected="true"]',
    );
    (selected ?? items?.[0])?.focus();

    function onPointerDown(event: PointerEvent) {
      if (!rootRef.current?.contains(event.target as Node)) setOpen(false);
    }
    document.addEventListener("pointerdown", onPointerDown);
    return () => document.removeEventListener("pointerdown", onPointerDown);
  }, [open]);

  function pick(next: T) {
    onValueChange(next);
    setOpen(false);
    triggerRef.current?.focus();
  }

  function onListKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    const items = Array.from(
      listRef.current?.querySelectorAll<HTMLElement>('[role="option"]') ?? [],
    );
    const index = items.indexOf(document.activeElement as HTMLElement);
    const move = (target: number) => {
      event.preventDefault();
      items[(target + items.length) % items.length]?.focus();
    };
    if (event.key === "ArrowDown") move(index + 1);
    else if (event.key === "ArrowUp") move(index - 1);
    else if (event.key === "Home") move(0);
    else if (event.key === "End") move(items.length - 1);
    else if (event.key === "Escape") {
      event.preventDefault();
      event.stopPropagation();
      setOpen(false);
      triggerRef.current?.focus();
    } else if (event.key === "Tab") setOpen(false);
  }

  return (
    <div ref={rootRef} className={cn("relative inline-block", className)}>
      <button
        ref={triggerRef}
        type="button"
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={open ? listId : undefined}
        onClick={() => setOpen((previous) => !previous)}
        onKeyDown={(event) => {
          if (event.key === "ArrowDown" && !open) {
            event.preventDefault();
            setOpen(true);
          }
        }}
        className={cn(
          "inline-flex items-center gap-1.5 rounded-xl border border-[var(--color-border)] bg-[var(--color-track)] text-[12.5px]",
          compact ? "h-8 px-2.5" : "h-[34px] px-3",
          "hover:text-[var(--color-text)] focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-[var(--color-primary)]",
        )}
      >
        <span
          className={
            compact ? "sr-only" : "font-medium text-[var(--color-text-muted)]"
          }
        >
          {label}:
        </span>{" "}
        <span className="font-semibold">{current?.label}</span>
        <ChevronDown
          aria-hidden="true"
          className={cn(
            "h-3.5 w-3.5 text-[var(--color-text-muted)] transition-transform",
            open && "rotate-180",
          )}
        />
      </button>

      {open ? (
        <div
          ref={listRef}
          id={listId}
          role="listbox"
          aria-label={label}
          onKeyDown={onListKeyDown}
          className={cn(
            "glass-card glass-card--popover scrollbar-glass absolute left-0 z-40 max-h-72 w-max min-w-full max-w-[min(20rem,calc(100vw-2rem))] overflow-y-auto overscroll-contain border border-[var(--color-border)] p-1.5",
            placement === "top" ? "bottom-full mb-2" : "top-full mt-2",
          )}
        >
          {options.map((option) => {
            const selected = option.value === value;
            return (
              <button
                key={option.value}
                type="button"
                role="option"
                aria-selected={selected}
                onClick={() => pick(option.value)}
                className={cn(
                  "flex w-full items-center justify-between gap-4 rounded-lg px-3 py-2 text-left text-[13px] transition-colors",
                  // Hover and keyboard focus tint the row with the accent so it visibly lifts off the glass (the
                  // neutral row-hover token is almost the same colour as the panel and showed nothing).
                  "hover:bg-[color-mix(in_srgb,var(--color-admin-teal)_16%,transparent)] focus-visible:bg-[color-mix(in_srgb,var(--color-admin-teal)_16%,transparent)] focus-visible:outline-none",
                  selected && "font-semibold",
                )}
              >
                {option.label}
                <Check
                  aria-hidden="true"
                  className={cn(
                    "h-3.5 w-3.5 shrink-0 text-[var(--color-primary)]",
                    !selected && "invisible",
                  )}
                />
              </button>
            );
          })}
        </div>
      ) : null}
    </div>
  );
}
