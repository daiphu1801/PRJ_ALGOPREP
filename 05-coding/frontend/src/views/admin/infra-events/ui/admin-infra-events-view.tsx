// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// ADM0404, split out of the queue screen on 2026-10-10: the machine-event feed was the densest block
// there and is read only when something went wrong. Read-only; filters combine with AND, the same
// way as the system log (search, level, service, day range inclusive on both ends).
//
// Boundary kept from BD ADM0401: these are machine logs, not administrative actions — the caption
// links to the system log for the latter. Rows are a timeline (not a DataTable) with a coloured left
// rule per level, like the system log, and "Tải thêm" instead of numbered pages.
//
// One render tree for mock and real data (2026-11-14): `useInfraEventFeed` returns the same shape in
// both modes. The real feed pages by cursor and does not know the total, so "n / tổng" is hidden there.
"use client";

import Link from "next/link";
import { useState } from "react";
import {
  INFRA_LEVELS,
  useInfraEventFeed,
  type InfraFilters,
  type InfraLevel,
} from "@/entities/infra-event";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import {
  Button,
  Card,
  EmptyState,
  FilterBar,
  FilterMenu,
  PageHeader,
  QueryError,
  Skeleton,
} from "@/shared/ui";
import { DateRangeFields, EventRow } from "./infra-event-parts";

const NO_FILTERS: InfraFilters = {
  q: "",
  level: "all",
  service: "all",
  from: "",
  to: "",
};

/** `initialLevel` seeds the level filter, e.g. from `?level=error` on the queue screen link. */
export function AdminInfraEventsView({
  initialLevel = "all",
}: {
  initialLevel?: InfraLevel | "all";
}) {
  const t = useT("adminInfraEvents");
  const [filters, setFilters] = useState<InfraFilters>({
    ...NO_FILTERS,
    level: initialLevel,
  });
  const feed = useInfraEventFeed(filters);
  const { events, total } = feed;

  function change(patch: Partial<InfraFilters>) {
    const next = { ...filters, ...patch };
    setFilters(next);
    if (next.from && next.to && next.from > next.to)
      toast.warning(t("rangeInvalid"));
  }

  const rangeInvalid = Boolean(
    filters.from && filters.to && filters.from > filters.to,
  );

  return (
    <div>
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        actions={
          <div className="flex items-center gap-2">
            {/* BD ADM0404 A NO3 / EVT-5: back to the queue screen (UX review B6). */}
            <Button
              asChild
              variant="ghost"
              size="sm"
              className="border border-[var(--color-border)]"
            >
              <Link href="/admin/queue">{t("backToQueue")}</Link>
            </Button>
            <Button
              variant="cta"
              size="sm"
              disabled={rangeInvalid || feed.isFetching}
              onClick={() => {
                void feed.refetch();
                toast.success(t("refreshDone"));
              }}
            >
              {t("refresh")}
            </Button>
          </div>
        }
      />

      <Card className="min-w-0 px-[18px] py-4">
        <p className="mb-3 text-[12.5px] text-[var(--color-text-muted)]">
          {t("caption")}{" "}
          <Link
            href="/admin/system-log"
            className="font-semibold text-[var(--color-primary)] hover:underline"
          >
            {t("captionLink")}
          </Link>
        </p>
        <FilterBar
          search={{
            label: t("searchLabel"),
            placeholder: t("searchPlaceholder"),
            value: filters.q,
            onChange: (q) => change({ q }),
            maxLength: 100,
          }}
          resultCount={
            total == null
              ? undefined
              : t("resultCount", { shown: events.length, total })
          }
        >
          <FilterMenu
            label={t("levelFilterLabel")}
            value={filters.level}
            onValueChange={(level) => change({ level })}
            options={[
              { value: "all" as const, label: t("levelAll") },
              ...INFRA_LEVELS.map((key) => ({
                value: key,
                label: t(`level.${key}`),
              })),
            ]}
          />
          <FilterMenu
            label={t("serviceFilterLabel")}
            value={filters.service}
            onValueChange={(service) => change({ service })}
            options={[
              { value: "all", label: t("serviceAll") },
              ...feed.services.map((name) => ({ value: name, label: name })),
            ]}
          />
          <DateRangeFields
            from={filters.from}
            to={filters.to}
            fromLabel={t("dateFrom")}
            toLabel={t("dateTo")}
            onFromChange={(from) => change({ from })}
            onToChange={(to) => change({ to })}
            invalid={rangeInvalid}
          />
        </FilterBar>

        {feed.error ? (
          <QueryError query={feed.query} />
        ) : feed.isLoading ? (
          <div className="flex flex-col gap-2" role="status">
            {Array.from({ length: 8 }, (_, index) => (
              <Skeleton key={index} className="h-[52px] w-full" />
            ))}
          </div>
        ) : events.length === 0 ? (
          <EmptyState>
            <span className="flex flex-col items-center gap-2">
              {t("emptyFiltered")}
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setFilters(NO_FILTERS)}
              >
                {t("clearFilters")}
              </Button>
            </span>
          </EmptyState>
        ) : (
          <ul aria-label={t("listLabel")} className="flex flex-col gap-0.5">
            {events.map((event) => (
              <EventRow
                key={event.id}
                event={event}
                levelLabel={t(`level.${event.level}`)}
              />
            ))}
          </ul>
        )}

        <div className="mt-1.5 flex items-center gap-3 border-t border-[var(--color-border)] px-2.5 pt-3">
          {total == null ? null : (
            <span className="text-[12.5px] text-[var(--color-text-muted)]">
              {t("pageLabel", { shown: events.length, total })}
            </span>
          )}
          <Button
            variant="ghost"
            size="sm"
            className="ml-auto border border-[var(--color-border)]"
            onClick={feed.loadMore}
            disabled={!feed.hasMore || feed.loadingMore}
          >
            {t("loadMore", { count: 20 })}
          </Button>
        </div>
      </Card>
    </div>
  );
}
