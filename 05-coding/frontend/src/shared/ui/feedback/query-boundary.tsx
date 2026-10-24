// Renders loading / error around a query and hands the loaded data to `children`. Lets a view that
// seeds local state from the data (`useState(data.rows)`) stay unchanged: put that view in an inner
// component that takes the data as a prop and wrap it here.
import type { ReactNode } from "react";
import { useT } from "@/shared/i18n";
import { Button } from "../primitives/button";
import { Skeleton } from "../primitives/skeleton";
import { ErrorState } from "./error-state";

type QueryLike<T> = {
  data: T | undefined;
  isLoading: boolean;
  isError: boolean;
  refetch: () => unknown;
};

type QueryBoundaryProps<T> = {
  query: QueryLike<T>;
  children: (data: T) => ReactNode;
  /** Replaces the default skeleton block. */
  loading?: ReactNode;
};

/** Error block with retry, for views that render their own loading state but must not hide a failure. */
export function QueryError({ query }: { query: Pick<QueryLike<unknown>, "refetch"> }) {
  const t = useT("common");
  return (
    <ErrorState>
      <div className="flex flex-col items-center gap-3">
        <p>{t("error")}</p>
        <Button variant="ghost" onClick={() => void query.refetch()}>
          {t("retry")}
        </Button>
      </div>
    </ErrorState>
  );
}

export function QueryBoundary<T>({
  query,
  children,
  loading,
}: QueryBoundaryProps<T>) {
  const t = useT("common");
  if (query.isError) return <QueryError query={query} />;
  if (query.isLoading || query.data === undefined) {
    return (
      loading ?? (
        <div role="status" aria-label={t("loading")} className="space-y-3">
          <Skeleton className="h-10 w-1/3" />
          <Skeleton className="h-64 w-full" />
        </div>
      )
    );
  }
  return <>{children(query.data)}</>;
}
