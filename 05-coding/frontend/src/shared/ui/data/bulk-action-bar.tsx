// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// "Đã chọn n ..." with the bulk actions, shown as a small glass pop-up floating at the top of the
// screen while rows are selected (owner instruction 2026-10-05). It used to be a bar inserted above the
// table (09-layoutBase/Admin - Quản lý bài tập.dc.html:196-206), which pushed the table down every time
// a row was ticked. Shared by the problem list and the account list; the screen owns the selection and
// passes the actions as children.
//
// Two placement details:
// - `.glass-card` carries a backdrop-filter, and a filtered ancestor becomes the containing block for
//   `position: fixed` descendants. Rendered in place inside a Card, "fixed" would pin to the card, not the
//   screen. So the pop-up is portalled out to the nearest shell root (`.admin-shell` / `.instructor-shell`),
//   which keeps those shells' glass and border tokens; it falls back to <body> outside any shell.
// - Screen readers do not announce a node that was just inserted, so an always-mounted status region
//   speaks the count.
"use client";

import { useState, type ReactNode } from "react";
import { createPortal } from "react-dom";

type BulkActionBarProps = {
  /** Rows currently selected. Nothing floats at 0. */
  count: number;
  /** Already-translated text, e.g. "Đã chọn 8 bài". */
  label: string;
  /** The action buttons. */
  children: ReactNode;
};

const SHELL_ROOT = ".admin-shell, .instructor-shell";

export function BulkActionBar({ count, label, children }: BulkActionBarProps) {
  // A callback ref into state: it also tells us when we are on the client and have a node to search from.
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);
  const target = anchor
    ? (anchor.closest<HTMLElement>(SHELL_ROOT) ?? document.body)
    : null;

  return (
    <>
      <div ref={setAnchor} role="status" className="sr-only">
        {count > 0 ? label : ""}
      </div>
      {count > 0 && target
        ? createPortal(
            <div
              role="region"
              aria-label={label}
              className="glass-card glass-card--popover bulk-pop-in fixed inset-x-4 top-3 z-40 mx-auto flex w-fit max-w-[calc(100vw-2rem)] flex-wrap items-center gap-x-4 gap-y-2 border border-[var(--color-border)] px-4 py-2.5"
            >
              <span className="text-[13px] font-semibold">{label}</span>
              <span className="flex flex-wrap gap-2">{children}</span>
            </div>,
            target,
          )
        : null}
    </>
  );
}
