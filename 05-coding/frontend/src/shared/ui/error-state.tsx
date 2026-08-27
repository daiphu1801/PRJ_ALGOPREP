import type { ReactNode } from "react";

export function ErrorState({ children }: { children: ReactNode }) {
  return (
    <div role="alert" className="flex min-h-[240px] items-center justify-center text-[var(--color-danger)]">
      {children}
    </div>
  );
}
