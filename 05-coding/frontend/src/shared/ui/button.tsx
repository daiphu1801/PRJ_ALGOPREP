import type { ComponentPropsWithoutRef, ReactElement } from "react";
import { cloneElement, isValidElement } from "react";
import { cn } from "@/shared/lib";

const VARIANT = {
  primary: "bg-[var(--color-primary)] text-[var(--color-on-primary)] hover:opacity-90",
  // The one dominant action on a screen. Reads --color-cta-*, which `.admin-shell` redefines as the
  // mockup's teal gradient (globals.css); `primary` cannot be used there because inside that scope
  // --color-primary is the near-white sidebar active fill and would vanish against a glass card.
  cta: "bg-[image:var(--color-cta-bg)] text-[var(--color-cta-fg)] shadow-[var(--color-cta-shadow)] hover:opacity-95",
  ghost: "bg-transparent text-[var(--color-text)] hover:bg-[var(--color-surface-hover)]",
} as const;

const SIZE = {
  sm: "h-8 px-3 text-sm",
  md: "h-10 px-4 text-sm",
} as const;

const BASE =
  "inline-flex items-center justify-center rounded-md font-medium whitespace-nowrap transition-colors disabled:opacity-50 disabled:pointer-events-none focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--color-primary)]";

type ButtonProps = ComponentPropsWithoutRef<"button"> & {
  variant?: keyof typeof VARIANT;
  size?: keyof typeof SIZE;
  /**
   * Forwards the Button's styling onto exactly one child element instead of rendering a
   * <button> tag. Use it when you need a link that looks like a button:
   * <Button asChild><Link href="...">…</Link></Button>.
   * Without it, every place that needs a link-styled-as-button would copy the class string —
   * exactly the kind of debt that compounds.
   */
  asChild?: boolean;
};

export function Button({
  variant = "primary",
  size = "md",
  className,
  asChild = false,
  children,
  ...props
}: ButtonProps) {
  const classes = cn(BASE, VARIANT[variant], SIZE[size], className);

  if (asChild && isValidElement<{ className?: string }>(children)) {
    const child = children as ReactElement<{ className?: string }>;
    return cloneElement(child, {
      className: cn(classes, child.props.className),
    });
  }

  return (
    <button className={classes} {...props}>
      {children}
    </button>
  );
}
