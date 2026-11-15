// PROTOTYPE — no DD hook yet for the alert counter. See 06-plan/PROTOTYPE_DEBT.md
//
// Data boundary of the infrastructure event log (ADM0404). `useInfraEventFeed` returns the same shape
// in mock and real mode: mock filters the fixture on the client, real asks the server per filter and
// pages by cursor, so the screen has a single render tree.
import { useState } from "react";
import { useInfiniteQuery } from "@tanstack/react-query";
import { isMockMode } from "@/shared/api";
import { useMockQuery } from "@/shared/api/mock-query";
import { fetchInfraEvents } from "./__mock__/infra-event-mocks";
import { readInfraEvents, type InfraFilters } from "./real-infra";
import type { InfraEvent } from "../model/types";

const SPEC = "03-dd/api/F4_judge-orchestration.md";
const PAGE_SIZE = 20;

/** `ListInfraEvents` over the whole fixture. Stale at once: the "Làm mới" button refetches it. */
const useMockInfraEvents = () =>
  useMockQuery({
    queryKey: ["infra-events"],
    enabled: isMockMode(),
    mock: fetchInfraEvents,
    spec: SPEC,
    staleTime: 0,
  });

function matches(event: InfraEvent, f: InfraFilters): boolean {
  const needle = f.q.trim().toLowerCase();
  return (
    (f.level === "all" || event.level === f.level) &&
    (f.service === "all" || event.service === f.service) &&
    (!f.from || event.date >= f.from) &&
    (!f.to || event.date <= f.to) &&
    (!needle ||
      `${event.message} ${event.service} ${event.id}`
        .toLowerCase()
        .includes(needle))
  );
}

export function useInfraEventFeed(filters: InfraFilters) {
  const rangeInvalid = Boolean(
    filters.from && filters.to && filters.from > filters.to,
  );
  const mock = useMockInfraEvents();
  const real = useInfiniteQuery({
    queryKey: ["judge-admin", "infra-events", filters],
    initialPageParam: undefined as string | undefined,
    queryFn: ({ pageParam }) => readInfraEvents(filters, pageParam),
    getNextPageParam: (page) => page.nextCursor ?? undefined,
    enabled: !isMockMode() && !rangeInvalid,
    retry: false,
  });

  // Mock paging restarts from the first page whenever a filter changes.
  const filterKey = JSON.stringify(filters);
  const [paging, setPaging] = useState({ key: filterKey, shown: PAGE_SIZE });
  const shown = paging.key === filterKey ? paging.shown : PAGE_SIZE;

  const query = isMockMode() ? mock : real;
  const pages = real.data?.pages ?? [];
  const matched = rangeInvalid
    ? []
    : (mock.data ?? []).filter((event) => matches(event, filters));
  const loaded = pages.flatMap((page) => page.events);
  const events: InfraEvent[] = isMockMode()
    ? matched.slice(0, shown)
    : loaded.map((event, index) => ({ ...event, id: `${index}` }));
  const services = isMockMode()
    ? [...new Set((mock.data ?? []).map((event) => event.service))].sort(
        (a, b) => a.localeCompare(b),
      )
    : [...new Set(pages.flatMap((page) => page.services))];

  return {
    events,
    services,
    /** Rows matching the filters, or null when the server does not say (cursor paging). */
    total: isMockMode() ? matched.length : null,
    hasMore: isMockMode() ? shown < matched.length : Boolean(real.hasNextPage),
    loadingMore: !isMockMode() && real.isFetchingNextPage,
    loadMore: () =>
      isMockMode()
        ? setPaging({ key: filterKey, shown: shown + PAGE_SIZE })
        : void real.fetchNextPage(),
    refetch: () => {
      setPaging({ key: filterKey, shown: PAGE_SIZE });
      return query.refetch();
    },
    isLoading: query.isLoading,
    isFetching: query.isFetching,
    error: query.error,
    query,
  };
}
