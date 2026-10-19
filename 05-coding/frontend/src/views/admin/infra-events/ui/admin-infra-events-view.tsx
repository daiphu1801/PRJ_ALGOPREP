// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// ADM0404, split out of the queue screen on 2026-10-10: the machine-event feed was the densest block
// there and is read only when something went wrong. Read-only; filters combine with AND, the same
// way as the system log (search, level, service, day range inclusive on both ends).
//
// Boundary kept from BD ADM0401: these are machine logs, not administrative actions — the caption
// links to the system log for the latter. Rows are a timeline (not a DataTable) with a coloured left
// rule per level, like the system log, and "Tải thêm" instead of numbered pages.
"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import {
  INFRA_LEVELS,
  fetchInfraEvents,
  type InfraEvent,
  type InfraLevel,
} from "@/entities/infra-event";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  EmptyState,
  FilterBar,
  FilterMenu,
  PageHeader,
  TextField,
  type BadgeVariant,
} from "@/shared/ui";

const PAGE_SIZE = 20;

const LEVEL_VARIANT: Record<InfraLevel, BadgeVariant> = {
  error: "negative",
  warn: "warn",
  info: "blue",
};

const LEVEL_COLOR_VAR: Record<InfraLevel, string> = {
  error: "--color-admin-negative",
  warn: "--color-admin-warn",
  info: "--color-accent-blue",
};

/** "2026-10-08" -> "08/10". */
function formatDay(date: string): string {
  const [, month, day] = date.split("-");
  return `${day}/${month}`;
}

/** `initialLevel` seeds the level filter, e.g. from `?level=error` on the queue screen link. */
export function AdminInfraEventsView({
  initialLevel = "all",
}: {
  initialLevel?: InfraLevel | "all";
}) {
  const t = useT("adminInfraEvents");
  const [all, setAll] = useState(fetchInfraEvents);
  const [query, setQuery] = useState("");
  const [level, setLevel] = useState<InfraLevel | "all">(initialLevel);
  const [service, setService] = useState("all");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [shown, setShown] = useState(PAGE_SIZE);

  const rangeInvalid = from !== "" && to !== "" && from > to;
  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return all.filter(
      (event) =>
        (level === "all" || event.level === level) &&
        (service === "all" || event.service === service) &&
        !rangeInvalid &&
        (!from || event.date >= from) &&
        (!to || event.date <= to) &&
        (!needle ||
          `${event.message} ${event.service} ${event.id}`
            .toLowerCase()
            .includes(needle)),
    );
  }, [all, query, level, service, from, to, rangeInvalid]);
  const events = filtered.slice(0, shown);

  const services = useMemo(
    () => [...new Set(all.map((event) => event.service))].sort(),
    [all],
  );

  // Any filter change goes back to the first page of results.
  function filterChange<V>(set: (value: V) => void) {
    return (value: V) => {
      set(value);
      setShown(PAGE_SIZE);
    };
  }

  function changeFrom(next: string) {
    filterChange(setFrom)(next);
    if (next && to && next > to) toast.warning(t("rangeInvalid"));
  }

  function changeTo(next: string) {
    filterChange(setTo)(next);
    if (next && from && from > next) toast.warning(t("rangeInvalid"));
  }

  function clearFilters() {
    setQuery("");
    setLevel("all");
    setService("all");
    setFrom("");
    setTo("");
    setShown(PAGE_SIZE);
  }

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
              onClick={() => {
                setAll(fetchInfraEvents());
                setShown(PAGE_SIZE);
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
            value: query,
            onChange: filterChange(setQuery),
          }}
          resultCount={t("resultCount", {
            shown: events.length,
            total: filtered.length,
          })}
        >
          <FilterMenu
            label={t("levelFilterLabel")}
            value={level}
            onValueChange={filterChange(setLevel)}
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
            value={service}
            onValueChange={filterChange(setService)}
            options={[
              { value: "all", label: t("serviceAll") },
              ...services.map((name) => ({ value: name, label: name })),
            ]}
          />
          {/* One unit, so the two ends wrap to the next line together rather than splitting. */}
          <span className="flex flex-wrap items-center gap-x-4 gap-y-2">
            <span className="flex items-center gap-1.5 text-[12.5px] font-medium whitespace-nowrap text-[var(--color-text-muted)]">
              {t("dateFrom")}:
              <TextField
                label={t("dateFrom")}
                hideLabel
                type="date"
                value={from}
                max={to || undefined}
                onChange={(event) => changeFrom(event.target.value)}
                wrapperClassName="w-[140px]"
                className="h-[34px]"
              />
            </span>
            <span className="flex items-center gap-1.5 text-[12.5px] font-medium whitespace-nowrap text-[var(--color-text-muted)]">
              {t("dateTo")}:
              <TextField
                label={t("dateTo")}
                hideLabel
                type="date"
                value={to}
                min={from || undefined}
                onChange={(event) => changeTo(event.target.value)}
                invalid={rangeInvalid}
                wrapperClassName="w-[140px]"
                className="h-[34px]"
              />
            </span>
          </span>
        </FilterBar>

        {events.length === 0 ? (
          <EmptyState>
            <span className="flex flex-col items-center gap-2">
              {t("emptyFiltered")}
              <Button variant="ghost" size="sm" onClick={clearFilters}>
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
          <span className="text-[12.5px] text-[var(--color-text-muted)]">
            {t("pageLabel", { shown: events.length, total: filtered.length })}
          </span>
          <Button
            variant="ghost"
            size="sm"
            className="ml-auto border border-[var(--color-border)]"
            onClick={() => setShown((previous) => previous + PAGE_SIZE)}
            disabled={shown >= filtered.length}
          >
            {t("loadMore", { count: PAGE_SIZE })}
          </Button>
        </div>
      </Card>
    </div>
  );
}

/** One timeline entry. The left rule only repeats the badge, so the badge carries the accessible text. */
function EventRow({
  event,
  levelLabel,
}: {
  event: InfraEvent;
  levelLabel: string;
}) {
  return (
    <li
      className="grid grid-cols-[66px_84px_minmax(180px,1fr)] items-start gap-3 rounded-xl border-l-2 px-2.5 py-2.5 hover:bg-[var(--color-row-hover)]"
      style={{ borderLeftColor: `var(${LEVEL_COLOR_VAR[event.level]})` }}
    >
      <span className="pt-px font-mono text-xs text-[var(--color-text-subtle)]">
        {event.time}
        <span className="block text-[10.5px]">{formatDay(event.date)}</span>
      </span>
      <Badge
        variant={LEVEL_VARIANT[event.level]}
        className="w-full justify-center font-mono"
      >
        {levelLabel}
      </Badge>
      <span className="min-w-0">
        <span className="block text-[13px] font-semibold text-pretty">
          {event.message}
        </span>
        <span className="mt-0.5 block font-mono text-[11.5px] text-[var(--color-text-subtle)]">
          {event.service}
        </span>
      </span>
    </li>
  );
}
