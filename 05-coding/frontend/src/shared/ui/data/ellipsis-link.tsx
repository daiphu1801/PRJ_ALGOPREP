// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// The title link of a list row: the clickable area is exactly the text, not the whole cell, and a title
// too long for its column is cut with an ellipsis and shows its full text as a tooltip. The tooltip is
// attached only when the text is really cut, so short titles do not grow a redundant one.
//
// The wrapper (`flex w-0 min-w-full`) lets the cell take the column's width from the table, instead of
// letting a long title widen the column and push the table past its card; the link is a flex item, so
// it is only as wide as its text and shrinks (with the ellipsis) when the column is narrower. A flex
// item is also block-level, which keeps the text on the same line as the other cells of the row.
// The column that holds it should declare a width, so its size does not depend on the title.
"use client";

import { useRef, useState, type ReactNode } from "react";
import Link from "next/link";
import { cn } from "@/shared/lib";

type EllipsisLinkProps = {
  href: string;
  children: ReactNode;
  className?: string;
};

export function EllipsisLink({ href, children, className }: EllipsisLinkProps) {
  const ref = useRef<HTMLAnchorElement>(null);
  const [title, setTitle] = useState<string | undefined>();

  // Measured when the pointer or focus arrives, before the browser shows its tooltip.
  function measure() {
    const element = ref.current;
    setTitle(
      element && element.scrollWidth > element.clientWidth
        ? (element.textContent ?? undefined)
        : undefined,
    );
  }

  return (
    <div className="flex w-0 min-w-full">
      <Link
        ref={ref}
        href={href}
        title={title}
        onMouseEnter={measure}
        onFocus={measure}
        className={cn("min-w-0 truncate hover:underline", className)}
      >
        {children}
      </Link>
    </div>
  );
}
