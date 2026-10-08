"use client";

import { useState, type MouseEventHandler } from "react";
import { createPortal } from "react-dom";
import Link from "next/link";
import type { LucideIcon } from "lucide-react";
import { Button } from "./button";
import { cn } from "@/shared/lib";

type IconActionProps = {
  icon: LucideIcon;
  /** Tooltip text, and the accessible name unless `ariaLabel` is given. */
  label: string;
  /** Accessible name with row context (e.g. "Sửa bài Word Break") when the tooltip stays short. */
  ariaLabel?: string;
  /** Renders a link instead of a <button>. */
  href?: string;
  /** With `href`: open in a new browser tab. */
  external?: boolean;
  onClick?: MouseEventHandler<HTMLElement>;
  tone?: "default" | "danger";
  /** Looks and acts disabled but stays hoverable, so the tooltip can say WHY it is disabled. */
  disabled?: boolean;
  className?: string;
};

/**
 * Square icon-only action (edit / duplicate / delete in list rows) with a small tooltip below it.
 *
 * The tooltip is visual only: the button's accessible name already carries the same words (the `label`,
 * or the longer `ariaLabel`), so it is not linked with aria-describedby, which would read them twice.
 *
 * The tooltip is portalled to <body> and positioned with `fixed` coordinates: the DataTable wrapper
 * is `overflow-x-auto`, which clips any in-flow tooltip on the last row.
 */
export function IconAction({
  icon: Icon,
  label,
  ariaLabel,
  href,
  external = false,
  onClick,
  tone = "default",
  disabled = false,
  className,
}: IconActionProps) {
  const [anchor, setAnchor] = useState<{ x: number; y: number } | null>(null);

  const show = (element: HTMLElement) => {
    const rect = element.getBoundingClientRect();
    setAnchor({ x: rect.left + rect.width / 2, y: rect.bottom + 6 });
  };
  const hide = () => setAnchor(null);

  const classes = cn(
    "h-8 w-8 border border-[var(--color-border)] px-0",
    tone === "danger" && "text-[var(--color-admin-negative)]",
    disabled && "cursor-not-allowed opacity-50",
    className,
  );
  const handlers = {
    onMouseEnter: (event: React.MouseEvent<HTMLElement>) =>
      show(event.currentTarget),
    onMouseLeave: hide,
    onFocus: (event: React.FocusEvent<HTMLElement>) =>
      show(event.currentTarget),
    onBlur: hide,
    "aria-label": ariaLabel ?? label,
    "aria-disabled": disabled || undefined,
  };
  const icon = <Icon aria-hidden="true" className="h-4 w-4" />;

  return (
    <>
      {href ? (
        <Button asChild variant="ghost" size="sm" className={classes}>
          <Link
            href={href}
            onClick={onClick}
            {...(external
              ? { target: "_blank", rel: "noopener noreferrer" }
              : {})}
            {...handlers}
          >
            {icon}
          </Link>
        </Button>
      ) : (
        <Button
          variant="ghost"
          size="sm"
          className={classes}
          onClick={disabled ? undefined : onClick}
          {...handlers}
        >
          {icon}
        </Button>
      )}
      {anchor
        ? createPortal(
            <span
              role="tooltip"
              style={{ left: anchor.x, top: anchor.y }}
              className="pointer-events-none fixed z-50 -translate-x-1/2 rounded-md bg-[var(--color-text)] px-2 py-1 text-[11px] leading-none font-medium whitespace-nowrap text-[var(--color-background)] shadow-md"
            >
              {label}
            </span>,
            document.body,
          )
        : null}
    </>
  );
}
