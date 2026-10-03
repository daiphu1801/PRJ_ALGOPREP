import type { ReactNode } from "react";

export function EmptyState({ children }: { children: ReactNode }) {
  return (
    <div role="status" className="flex min-h-[240px] items-center justify-center text-[var(--color-text-muted)]">
      {children}
    </div>
  );
}
