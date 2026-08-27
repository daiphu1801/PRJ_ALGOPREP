import type { ComponentPropsWithoutRef } from "react";
import { cn } from "@/shared/lib";

export function Skeleton({ className, ...props }: ComponentPropsWithoutRef<"div">) {
  return (
    <div
      className={cn("animate-pulse rounded-md bg-[var(--color-surface-hover)]", className)}
      aria-hidden="true"
      {...props}
    />
  );
}
