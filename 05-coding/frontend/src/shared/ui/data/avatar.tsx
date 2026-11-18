"use client";

import { useState } from "react";
import { cn } from "@/shared/lib/cn";
import { initialsOf } from "@/shared/lib/initials";

const SIZES = {
  sm: "h-8 w-8 text-[11.5px]",
  md: "h-10 w-10 text-sm",
  lg: "h-[68px] w-[68px] text-xl",
} as const;

export type AvatarSize = keyof typeof SIZES;

/**
 * Round person picture: the image when `src` is set and decodes, the initials otherwise. Pure
 * presentation: no API call, no i18n (BD USR0502 Khu vực A NO 1). The full name is the accessible
 * name so the initials fallback stays readable to a screen reader.
 */
export function Avatar({
  src,
  name,
  size = "md",
  className,
}: {
  src?: string | null;
  name: string;
  size?: AvatarSize;
  className?: string;
}) {
  // Remember which src failed so a new src gets a fresh try without an effect.
  const [failedSrc, setFailedSrc] = useState<string | null>(null);
  const showImage = Boolean(src) && failedSrc !== src;
  return (
    <span
      role="img"
      aria-label={name}
      className={cn(
        "inline-flex shrink-0 items-center justify-center overflow-hidden rounded-full border border-[var(--color-border)] bg-[var(--color-surface)] font-semibold text-[var(--color-text-muted)]",
        SIZES[size],
        className,
      )}
    >
      {showImage ? (
        // eslint-disable-next-line @next/next/no-img-element -- a blob: URL, next/image cannot optimise it
        <img
          src={src as string}
          alt=""
          className="h-full w-full object-cover"
          onError={() => setFailedSrc(src as string)}
        />
      ) : (
        <span aria-hidden="true">{initialsOf(name) || "?"}</span>
      )}
    </span>
  );
}
