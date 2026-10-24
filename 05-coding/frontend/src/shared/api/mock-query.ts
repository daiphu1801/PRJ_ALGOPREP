// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Shared data boundary for every screen that still runs on mock data. An entity or view keeps its
// own `api/__mock__/` folder and writes one line per endpoint here, instead of repeating the
// `withMockData(mock, notImplemented)` + `useQuery` boilerplate. Graduation of an endpoint = replace
// `notImplemented(spec)` with the real fetcher; the call sites do not change.
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { QueryKey } from "@tanstack/react-query";
import { ApiError } from "./api-error";
import { withMockData } from "./mock-mode";

/** `fetchReal` stub: fails loudly with the DD file that still has to define the endpoint. */
export function notImplemented(spec: string): () => never {
  return () => {
    throw new ApiError(
      "NOT_IMPLEMENTED",
      501,
      `${spec} chưa định nghĩa endpoint này`,
    );
  };
}

type MockQueryArgs<T> = {
  queryKey: QueryKey;
  mock: () => Promise<T> | T;
  /** DD file that will define the endpoint, e.g. `03-dd/api/F2_problem-bank.md`. */
  spec: string;
  /** Default 30s; pass 0 where a reopened screen must show what was last saved. */
  staleTime?: number;
  enabled?: boolean;
};

export function useMockQuery<T>({
  queryKey,
  mock,
  spec,
  staleTime = 30_000,
  enabled,
}: MockQueryArgs<T>) {
  return useQuery({
    queryKey,
    queryFn: () => withMockData(mock, notImplemented(spec)),
    staleTime,
    retry: false,
    enabled,
  });
}

type MockMutationArgs<V, T> = {
  mock: (variables: V) => Promise<T> | T;
  spec: string;
  /** Query keys refreshed after a successful call. */
  invalidate?: readonly QueryKey[];
};

export function useMockMutation<V, T = void>({
  mock,
  spec,
  invalidate = [],
}: MockMutationArgs<V, T>) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (variables: V) =>
      withMockData(() => mock(variables), notImplemented(spec)),
    onSuccess: () =>
      Promise.all(
        invalidate.map((queryKey) =>
          queryClient.invalidateQueries({ queryKey }),
        ),
      ),
  });
}
