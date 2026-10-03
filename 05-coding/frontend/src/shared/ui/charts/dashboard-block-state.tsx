// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Wraps ONE dashboard block with its own loading/empty/error state — per BD, each of the 9 blocks
// on admin_overview manages its 3 states independently, so one failing source never blocks the
// rest of the screen (02-bd/screens/admin/ADM0101_overview.md section 4,
// DEC-2026-0831-admin-overview-ui-decisions). Purely presentational — no admin/dashboard domain
// type — so it lives in shared/ui per SYS0102_frontend_architecture.md section 2.A-B.
import type { ReactNode } from "react";
import { toast } from "@/shared/lib/toast-store";
import { Button, EmptyState, ErrorState, Skeleton } from "@/shared/ui";

type DashboardBlockStateProps = {
  title: string;
  isLoading: boolean;
  isError: boolean;
  isEmpty: boolean;
  /** May return the refetch promise (react-query); a result with `isError` means it failed again. */
  onRetry: () => unknown;
  emptyMessage: string;
  errorMessage: string;
  retryLabel: string;
  /** Height hint so the loading skeleton doesn't shift layout once real content arrives. */
  minHeightClassName?: string;
  /** Compact stat cards already show their own label — skip the redundant section heading. */
  hideTitle?: boolean;
  children: ReactNode;
};

/** react-query resolves a failed refetch instead of throwing, with `isError` set on the result. */
function failedAgain(result: unknown): boolean {
  return typeof result === "object" && result !== null && "isError" in result && result.isError === true;
}

export function DashboardBlockState({
  title,
  isLoading,
  isError,
  isEmpty,
  onRetry,
  emptyMessage,
  errorMessage,
  retryLabel,
  minHeightClassName = "min-h-[176px]",
  hideTitle = false,
  children,
}: DashboardBlockStateProps) {
  // The block already says it failed. A retry the user just pressed that fails again is a new event,
  // so it gets a toast; the first load failure does not (a dashboard can have many blocks failing).
  async function retry() {
    if (failedAgain(await onRetry())) toast.error(errorMessage);
  }

  return (
    <section
      aria-label={title}
      className={`glass-card p-3 ${minHeightClassName}`}
    >
      {!hideTitle && <h3 className="mb-2 text-sm font-semibold text-[var(--color-text)]">{title}</h3>}
      {isLoading ? (
        <Skeleton className={minHeightClassName} />
      ) : isError ? (
        <ErrorState>
          <div className="flex flex-col items-center gap-2">
            <span>{errorMessage}</span>
            <Button size="sm" variant="ghost" onClick={() => void retry()}>
              {retryLabel}
            </Button>
          </div>
        </ErrorState>
      ) : isEmpty ? (
        <EmptyState>{emptyMessage}</EmptyState>
      ) : (
        children
      )}
    </section>
  );
}
