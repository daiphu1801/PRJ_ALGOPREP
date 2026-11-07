"use client";
import Link from "next/link";
import { useState } from "react";
import { useInfiniteQuery } from "@tanstack/react-query";
import { api } from "@/shared/api";
import { useLocale, useT } from "@/shared/i18n";
import {
  Button,
  Card,
  EmptyState,
  FilterMenu,
  PageHeader,
  QueryError,
  TextField,
} from "@/shared/ui";
import type { InfraLevel } from "@/entities/infra-event";
type EventPage = {
  events: {
    occurredAt: string;
    level: string;
    message: string;
    service: string;
  }[];
  nextCursor: string | null;
  services: string[];
};
export function RealInfraEvents({
  initialLevel,
}: {
  initialLevel: InfraLevel | "all";
}) {
  const t = useT("adminInfraEvents");
  const tc = useT("common");
  const locale = useLocale();
  const [q, setQ] = useState("");
  const [level, setLevel] = useState<string>(initialLevel);
  const [service, setService] = useState("all");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const invalid = Boolean(from && to && from > to);
  const query = useInfiniteQuery({
    queryKey: ["judge-admin", "infra-events", q, level, service, from, to],
    initialPageParam: undefined as string | undefined,
    queryFn: ({ pageParam }) =>
      api.get<EventPage>("/judge/admin/infra-events", {
        q,
        level: level === "all" ? undefined : level.toUpperCase(),
        service: service === "all" ? undefined : service,
        from: from || undefined,
        to: to || undefined,
        cursor: pageParam,
      }),
    getNextPageParam: (page) => page.nextCursor ?? undefined,
    enabled: !invalid,
    retry: false,
  });
  const events = query.data?.pages.flatMap((page) => page.events) ?? [];
  const services = [
    ...new Set(query.data?.pages.flatMap((page) => page.services) ?? []),
  ];
  return (
    <div className="space-y-4">
      <PageHeader title={t("title")} description={t("subtitle")} />
      <Link href="/admin/queue">{t("backToQueue")}</Link>
      <Card className="space-y-4">
        <div className="flex flex-wrap gap-3">
          <TextField
            label={t("searchLabel")}
            value={q}
            maxLength={100}
            onChange={(event) => setQ(event.target.value)}
          />
          <FilterMenu
            label={t("levelFilterLabel")}
            value={level}
            onValueChange={setLevel}
            options={[
              { value: "all", label: t("levelAll") },
              ...["error", "warn", "info"].map((value) => ({
                value,
                label: t(`level.${value}`),
              })),
            ]}
          />
          <FilterMenu
            label={t("serviceFilterLabel")}
            value={service}
            onValueChange={setService}
            options={[
              { value: "all", label: t("serviceAll") },
              ...services.map((value) => ({ value, label: value })),
            ]}
          />
          <TextField
            label={t("dateFrom")}
            type="date"
            value={from}
            onChange={(event) => setFrom(event.target.value)}
          />
          <TextField
            label={t("dateTo")}
            type="date"
            value={to}
            onChange={(event) => setTo(event.target.value)}
          />
          <Button
            disabled={invalid || query.isFetching}
            onClick={() => void query.refetch()}
          >
            {t("refresh")}
          </Button>
        </div>
        {invalid ? (
          <p>{t("rangeInvalid")}</p>
        ) : query.isError ? (
          <QueryError query={query} />
        ) : query.isLoading ? (
          <p role="status">{tc("loading")}</p>
        ) : (
          <>
            {events.length === 0 ? (
              <EmptyState>{t("emptyFiltered")}</EmptyState>
            ) : null}
            <ul aria-label={t("listLabel")} className="space-y-3">
              {events.map((event, index) => (
                <li
                  key={`${event.occurredAt}:${index}`}
                  className="border-l-2 border-[var(--color-border)] pl-3"
                >
                  <p className="text-sm">
                    {t(`level.${event.level.toLowerCase()}`)} · {event.service}{" "}
                    ·{" "}
                    {new Date(event.occurredAt).toLocaleString(locale, {
                      timeZone: "Asia/Ho_Chi_Minh",
                    })}
                  </p>
                  <p className="whitespace-pre-wrap break-words">
                    {event.message}
                  </p>
                </li>
              ))}
            </ul>
            {query.hasNextPage ? (
              <Button
                disabled={query.isFetchingNextPage}
                onClick={() => void query.fetchNextPage()}
              >
                {t("loadMore", { count: 20 })}
              </Button>
            ) : null}
          </>
        )}
      </Card>
    </div>
  );
}
