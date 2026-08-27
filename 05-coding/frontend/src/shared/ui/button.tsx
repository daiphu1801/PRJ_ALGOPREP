import type { ComponentPropsWithoutRef, ReactElement } from "react";
import { cloneElement, isValidElement } from "react";
import { cn } from "@/shared/lib";

const VARIANT = {
  primary: "bg-[var(--color-primary)] text-[var(--color-on-primary)] hover:opacity-90",
  ghost: "bg-transparent text-[var(--color-text)] hover:bg-[var(--color-surface-hover)]",
} as const;

const SIZE = {
  sm: "h-8 px-3 text-sm",
  md: "h-10 px-4 text-sm",
} as const;

const BASE =
  "inline-flex items-center justify-center rounded-md font-medium transition-colors disabled:opacity-50 disabled:pointer-events-none focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--color-primary)]";

type ButtonProps = ComponentPropsWithoutRef<"button"> & {
  variant?: keyof typeof VARIANT;
  size?: keyof typeof SIZE;
  /**
   * Truyền style của Button xuống đúng một element con thay vì render thẻ <button>.
   * Dùng khi cần một liên kết trông như nút: <Button asChild><Link href="...">…</Link></Button>.
   * Không có nó thì mọi chỗ cần link-dạng-nút sẽ copy lại chuỗi class — đúng loại nợ sinh sôi.
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
