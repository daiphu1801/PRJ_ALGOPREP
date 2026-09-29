// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// The one responsive container all three actor shells use (owner instruction 2026-09-27,
// DEC-2026-0927-student-area-merge-and-shared-shell). Replaces the two hard-coded `max-w-[…px]`
// wrappers that used to live inside app-shell.tsx.
//
// Responsive in both directions, which a bare `max-w-*` utility is not:
// - ABOVE the cap it stops growing and centres, so text lines stay readable on a 4K monitor.
// - BELOW it the container is fluid and the horizontal gutter SHRINKS by breakpoint (24px on a
//   desktop, 12px on a phone). A fixed gutter is what makes a narrow viewport feel cramped —
//   percentage of the screen goes to padding instead of content.
//
// The cap is a plain number in px so it can be tuned per area from `shared/config/layout.ts`
// without touching any JSX, and overridden per screen for the rare one that needs it.
import type { ReactNode } from "react";
import { cn } from "@/shared/lib";

export function PageContainer({
  maxWidthPx,
  className,
  children,
}: {
  /** Pixel cap. Omit for a full-bleed container that only contributes the responsive gutter. */
  maxWidthPx?: number;
  className?: string;
  children: ReactNode;
}) {
  return (
    <div
      style={maxWidthPx ? { maxWidth: maxWidthPx } : undefined}
      className={cn("mx-auto w-full px-3 sm:px-4 lg:px-6", className)}
    >
      {children}
    </div>
  );
}
