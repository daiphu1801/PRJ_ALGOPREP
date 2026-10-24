import Link from "next/link";
import { ArrowRight } from "lucide-react";
import type { ReactNode } from "react";
import { cn } from "@/shared/lib/cn";

/**
 * Closing link of a card or dashboard block ("Tất cả", "Xem chi tiết"): pill button with an arrow
 * instead of a bare underlined word. `className` overrides the default top margin when the link
 * sits in a flex row.
 */
export function SeeAllLink({
  href,
  children,
  className,
  "aria-label": ariaLabel,
}: {
  href: string;
  children: ReactNode;
  className?: string;
  "aria-label"?: string;
}) {
  return (
    <Link
      href={href}
      aria-label={ariaLabel}
      className={cn(
        "group mt-3 inline-flex items-center gap-1.5 rounded-full border border-[var(--color-border)] px-3.5 py-1.5 text-[12.5px] font-semibold transition-colors hover:border-[var(--color-primary)] hover:text-[var(--color-primary)]",
        className,
      )}
    >
      {children}
      <ArrowRight
        aria-hidden="true"
        className="size-3.5 transition-transform group-hover:translate-x-0.5"
      />
    </Link>
  );
}
