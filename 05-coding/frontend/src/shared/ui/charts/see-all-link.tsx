import Link from "next/link";
import { ArrowRight } from "lucide-react";
import type { ReactNode } from "react";

/** Footer link of a dashboard block ("Tất cả"): pill button with an arrow, not a bare underlined word. */
export function SeeAllLink({ href, children }: { href: string; children: ReactNode }) {
  return (
    <Link
      href={href}
      className="group mt-3 inline-flex items-center gap-1.5 rounded-full border border-[var(--color-border)] px-3.5 py-1.5 text-[12.5px] font-semibold transition-colors hover:border-[var(--color-primary)] hover:text-[var(--color-primary)]"
    >
      {children}
      <ArrowRight
        aria-hidden="true"
        className="size-3.5 transition-transform group-hover:translate-x-0.5"
      />
    </Link>
  );
}
