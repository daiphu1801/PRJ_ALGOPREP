// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md.
//
// F6-03 "đánh dấu xem lại" — a binary per-question, per-user flag. Prototype (dc.html) doesn't
// have this control at all; both BD files add it because RD put F6-03 in scope [SoT:
// 02-bd/screens/users/USR0401_interview_bank_list.md Sheet 5 khu vực C.6].
"use client";

import { Bookmark } from "lucide-react";
import { cn } from "@/shared/lib";

export function BookmarkToggle({
  bookmarked,
  onToggle,
  label,
  className,
}: {
  bookmarked: boolean;
  onToggle: () => void;
  label: string;
  className?: string;
}) {
  return (
    <button
      type="button"
      aria-pressed={bookmarked}
      aria-label={label}
      title={label}
      onClick={onToggle}
      className={cn(
        "inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border transition-colors",
        "focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-[var(--color-primary)]",
        bookmarked
          ? "border-[var(--color-primary)] text-[var(--color-primary)]"
          : "border-[var(--color-border)] text-[var(--color-text-muted)] hover:text-[var(--color-text)]",
        className,
      )}
    >
      <Bookmark className="h-4 w-4" fill={bookmarked ? "currentColor" : "none"} aria-hidden="true" />
    </button>
  );
}
