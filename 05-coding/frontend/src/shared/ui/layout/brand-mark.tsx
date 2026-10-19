import Image from "next/image";
import { cn } from "@/shared/lib";

type BrandMarkProps = {
  /** Rendered edge length in px; the source PNG is 512x512 with a baked-in dark outline. */
  size?: number;
  className?: string;
};

/**
 * The AlgoPrep logo mark (`public/logo-mark.png`), cut from `05-coding/img-system/logoPrj.png` with a
 * transparent background and a thick navy outline so the white "P" stays visible on both light and
 * dark surfaces. Decorative: every caller already prints the "AlgoPrep" wordmark as text next to it.
 */
export function BrandMark({ size = 28, className }: BrandMarkProps) {
  return (
    <Image
      src="/logo-mark.png"
      alt=""
      width={size}
      height={size}
      className={cn("shrink-0 select-none", className)}
      draggable={false}
    />
  );
}
