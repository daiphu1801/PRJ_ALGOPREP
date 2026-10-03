// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Glass panel with an optional title/description head and an action slot. Every Admin mockup builds
// its content out of these: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html uses four on one
// screen (:163, :206, :221, :238), all with the same padding (18px 20px) and 22px radius.
//
// The `.glass-card` class itself already lives in globals.css — this component exists for the head
// row (title + description + trailing action), which was being hand-rolled identically per card.
import type { ReactNode } from "react";
import { cn } from "@/shared/lib";

type CardProps = {
  title?: ReactNode;
  description?: ReactNode;
  /** Trailing control on the head row (a link, a button, a filter). */
  action?: ReactNode;
  children: ReactNode;
  className?: string;
  /** Head is a <h2> by default; pass 3 where the card sits under another heading. */
  headingLevel?: 2 | 3;
};

export function Card({
  title,
  description,
  action,
  children,
  className,
  headingLevel = 2,
}: CardProps) {
  const Heading = headingLevel === 2 ? "h2" : "h3";

  return (
    <section
      className={cn(
        "glass-card border border-[var(--color-border)] px-5 py-[18px]",
        className,
      )}
    >
      {title || action ? (
        <div className="mb-3 flex items-start justify-between gap-4">
          <div className="min-w-0">
            {title ? <Heading className="text-[14.5px] font-bold">{title}</Heading> : null}
            {description ? (
              <p className="text-[12.5px] text-[var(--color-text-muted)]">{description}</p>
            ) : null}
          </div>
          {action ? <div className="shrink-0">{action}</div> : null}
        </div>
      ) : null}
      {children}
    </section>
  );
}
