// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// 8 of the 13 Admin/shared BD files describe a modal. Note it is NOT in the static mockups —
// 02-bd/screens/admin/admin_user_management.md section 3 adds the `bulk-action-confirming` state
// and flags it [SoT: Suy luận] precisely because 09-layoutBase never drew one. So the BD, not the
// prototype, is the source here.
//
// Focus handling is the reason this is a shared primitive rather than a per-screen div: a dialog
// that does not trap focus, close on Escape, or restore focus to its opener is a keyboard trap.
"use client";

import { useEffect, useRef, type ReactNode } from "react";
import { cn } from "@/shared/lib";

const FOCUSABLE =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

type ModalProps = {
  open: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
  /** Action row, rendered bottom-right. */
  footer?: ReactNode;
  className?: string;
};

export function Modal({ open, onClose, title, children, footer, className }: ModalProps) {
  const panelRef = useRef<HTMLDivElement>(null);
  const openerRef = useRef<HTMLElement | null>(null);

  useEffect(() => {
    if (!open) return;

    openerRef.current = document.activeElement as HTMLElement | null;
    const panel = panelRef.current;
    panel?.querySelector<HTMLElement>(FOCUSABLE)?.focus();

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        event.stopPropagation();
        onClose();
        return;
      }
      if (event.key !== "Tab" || !panel) return;

      const focusable = Array.from(panel.querySelectorAll<HTMLElement>(FOCUSABLE));
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (!first || !last) return;

      const active = document.activeElement;

      // Wrap at both ends, so Tab never escapes the dialog into the page behind it.
      if (event.shiftKey && (active === first || !panel.contains(active))) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && active === last) {
        event.preventDefault();
        first.focus();
      }
    }

    document.addEventListener("keydown", onKeyDown, true);
    return () => {
      document.removeEventListener("keydown", onKeyDown, true);
      openerRef.current?.focus();
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        className="absolute inset-0 bg-[var(--color-scrim)]"
        aria-hidden="true"
        onClick={onClose}
      />
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className={cn(
          "glass-card relative z-10 w-full max-w-md border border-[var(--color-border)] p-5",
          className,
        )}
      >
        <h2 className="text-base font-semibold">{title}</h2>
        <div className="mt-3 text-sm text-[var(--color-text-muted)]">{children}</div>
        {footer ? <div className="mt-5 flex justify-end gap-2">{footer}</div> : null}
      </div>
    </div>
  );
}
