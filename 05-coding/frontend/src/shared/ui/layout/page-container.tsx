import type { ReactNode } from "react";
import { SHELL_PADDING_X, SHELL_PADDING_Y } from "@/shared/api/config";
import { cn } from "@/shared/lib";

export function PageContainer({
  variant = "frame",
  className,
  children,
}: {
  variant?: "frame" | "content" | "viewport";
  className?: string;
  children: ReactNode;
}) {
  return (
    <div
      data-page-viewport={variant === "viewport" ? "" : undefined}
      className={cn(
        "w-full min-w-0",
        variant === "frame"
          ? [SHELL_PADDING_X, SHELL_PADDING_Y]
          : "flex min-h-0 flex-1 flex-col [&>*:only-child]:grow",
        className,
      )}
    >
      {children}
    </div>
  );
}
