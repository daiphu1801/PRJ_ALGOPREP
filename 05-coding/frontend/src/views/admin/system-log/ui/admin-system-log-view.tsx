// PROTOTYPE with a real API behind it (task 12, 03-dd/api/F1_identity.md #39, #40, #64 to #66, #51, #52);
// see 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Layout follows 09-layoutBase/Admin - Nhật ký hệ thống.dc.html: sticky header with a live toggle
// and an export CTA (:149-163), then a two-column grid 1fr / minmax(290px,0.42fr) (:179) — event list
// left (:181-214), two side panels right (:216-246). The stat strip is gone (DEC-2026-1001-single-
// overview-page-kpi).
//
// Everything belonging to the removed rejudge feature is stripped, per
// 02-bd/screens/admin/admin_system_log.md section 0 and DEC-2026-0828-remove-rejudge-scope.
//
// The event list is deliberately NOT a DataTable: BD section 2 specifies a timeline whose rows wrap
// onto two lines with a category-coloured left rule, plus "Tải thêm 50 dòng" instead of numbered
// paging — a column grid would fight that shape rather than help it.
"use client";

import { useMemo, useState } from "react";
import {
  AUDIT_CATEGORIES,
  LOAD_MORE_COUNT,
  initialsOf,
  useActiveAdmins,
  useAuditLive,
  useAuditLog,
  useCategoryBreakdown,
  useLogRetention,
  useSaveLogRetention,
  type AuditCategory,
  type AuditEvent,
} from "../api";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import { textTone, useDebounced } from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import type { ExpiryPolicy } from "../model/settings";
import { ExportLogDialog } from "./export-log-dialog";
import {
  Badge,
  Button,
  Card,
  EmptyState,
  PageHeader,
  ParamsDialog,
  QueryBoundary,
  QueryError,
  RankedProgressList,
  FilterBar,
  FilterMenu,
  SettingRow,
  Skeleton,
  TextField,
  type BadgeVariant,
} from "@/shared/ui";

// dc.html:386-390 maps each category to its own accent. `config` and `content` land on tokens the
// project already has (warn, success); `auth` and `permission` use the blue/purple ones added with
// this screen.
const CATEGORY_VARIANT: Record<AuditCategory, BadgeVariant> = {
  auth: "blue",
  permission: "purple",
  config: "warn",
  content: "success",
};

const CATEGORY_COLOR_VAR: Record<AuditCategory, string> = {
  auth: "--color-accent-blue",
  permission: "--color-accent-purple",
  config: "--color-admin-warn",
  content: "--color-success",
};

type CategoryFilter = AuditCategory | "all";

/** "2026-10-08" -> "08/10". */
function formatDay(date: string): string {
  const [, month, day] = date.split("-");
  return `${day}/${month}`;
}

/** Turns the parameter bag of a row into the primitives a message template accepts. */
function templateValues(params: Record<string, unknown>) {
  const values: Record<string, string | number> = {};
  for (const [key, value] of Object.entries(params)) {
    if (typeof value === "string" || typeof value === "number") {
      values[key] = value;
    } else if (typeof value === "boolean") {
      values[key] = String(value);
    }
  }
  return values;
}

export function AdminSystemLogView() {
  const t = useT("adminSystemLog");
  const toMessage = useApiErrorMessage();
  const saveRetention = useSaveLogRetention();
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState<CategoryFilter>("all");
  // ADM0403 Q6: day range, inclusive on both ends. Empty string = open end.
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [live, setLive] = useState(true);
  const [editingRetention, setEditingRetention] = useState(false);
  const [exporting, setExporting] = useState(false);

  const debouncedQuery = useDebounced(query);
  const rangeInvalid = from !== "" && to !== "" && from > to;
  // The server filters: text over service, actor and code (the BD rules out a full-text scan over the
  // message column on a large table, BD section 2), category and day range combine with AND.
  const params = useMemo(
    () => ({ query: debouncedQuery, category, from, to }),
    [debouncedQuery, category, from, to],
  );
  const list = useAuditLog(params, !rangeInvalid);
  const liveFeed = useAuditLive(live, params, (error) =>
    toast.error(toMessage(error)),
  );
  const activeAdmins = useActiveAdmins();
  const breakdown = useCategoryBreakdown();
  const retention = useLogRetention();

  // A row can sit in a page and in the live prepend at once: keep the first.
  const events = useMemo(() => {
    if (rangeInvalid) return [];
    const seen = new Set<string>();
    return (list.data?.pages ?? [])
      .flatMap((page) => page.events)
      .filter((event) => !seen.has(event.id) && seen.add(event.id));
  }, [list.data, rangeInvalid]);

  function describe(event: AuditEvent): string {
    if (event.message) return event.message;
    if (!event.action) return "";
    const key = `action.${event.action.type}`;
    if (t.has(key)) {
      try {
        return t(key, templateValues(event.action.params));
      } catch {
        // A parameter the template needs is missing: the generic sentence below is still true.
      }
    }
    return t("actionFallback", { action: event.action.type });
  }

  function changeFrom(next: string) {
    setFrom(next);
    if (next && to && next > to) toast.warning(t("rangeInvalid"));
  }

  function changeTo(next: string) {
    setTo(next);
    if (next && from && from > next) toast.warning(t("rangeInvalid"));
  }

  const categoryOptions = [
    { value: "all" as const, label: t("categoryAll") },
    ...AUDIT_CATEGORIES.map((key) => ({
      value: key,
      label: t(`category.${key}`),
    })),
  ];

  const settings = retention.data;

  return (
    <div>
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        actions={
          <>
            {live && liveFeed.status === "reconnecting" ? (
              <span
                role="status"
                className="text-[12.5px] text-[var(--color-text-muted)]"
              >
                {t("liveReconnecting")}
              </span>
            ) : null}
            <Button
              variant="ghost"
              size="sm"
              aria-pressed={live}
              onClick={() => setLive((previous) => !previous)}
              className="border border-[var(--color-border)]"
            >
              <span
                aria-hidden="true"
                className={`mr-2 inline-block h-[7px] w-[7px] rounded-full ${
                  live
                    ? "animate-pulse bg-[var(--color-success)]"
                    : "bg-[var(--color-text-subtle)]"
                }`}
              />
              {live ? t("liveOn") : t("liveOff")}
            </Button>
            <Button
              variant="ghost"
              size="sm"
              className="border border-[var(--color-border)]"
              onClick={() => setEditingRetention(true)}
              disabled={!settings}
            >
              {t("retention.open")}
            </Button>
            <Button
              variant="cta"
              size="sm"
              onClick={() => setExporting(true)}
              disabled={!settings}
            >
              {t("export")}
            </Button>
          </>
        }
      />
      {/* New rows are announced in one polite line, not read out one by one (DD section 6). */}
      <span className="sr-only" aria-live="polite">
        {liveFeed.received > 0
          ? t("liveAnnounce", { count: liveFeed.received })
          : ""}
      </span>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(290px,0.42fr)]">
        <Card className="min-w-0 px-[18px] py-4">
          <FilterBar
            search={{
              label: t("searchLabel"),
              placeholder: t("searchPlaceholder"),
              value: query,
              onChange: (next) => setQuery(next),
            }}
            resultCount={t("resultCount", { count: events.length })}
          >
            <FilterMenu
              label={t("categoryFilterLabel")}
              options={categoryOptions}
              value={category}
              onValueChange={setCategory}
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

          {list.isError && !list.data ? (
            <QueryError query={list} />
          ) : list.isPending && !rangeInvalid ? (
            <div role="status" className="space-y-2 py-2">
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-12 w-full" />
            </div>
          ) : events.length === 0 ? (
            <EmptyState>
              <span className="flex flex-col items-center gap-2">
                {t("emptyFiltered")}
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    setQuery("");
                    setCategory("all");
                    setFrom("");
                    setTo("");
                  }}
                >
                  {t("clearFilters")}
                </Button>
              </span>
            </EmptyState>
          ) : (
            <ul
              aria-label={t("eventListLabel")}
              className="flex flex-col gap-0.5"
            >
              {events.map((event) => (
                <EventRow
                  key={event.id}
                  event={event}
                  message={describe(event)}
                  categoryLabel={t(`category.${event.category}`)}
                  reasonLabel={t("reasonLabel")}
                />
              ))}
            </ul>
          )}

          <div className="mt-1.5 flex items-center gap-3 border-t border-[var(--color-border)] px-2.5 pt-3">
            <span className="text-[12.5px] text-[var(--color-text-muted)]">
              {t("pageLabel", { shown: events.length })}
            </span>
            {list.isFetchNextPageError ? (
              <span className="text-[12.5px] text-[var(--color-danger)]">
                {t("loadMoreFailed")}
              </span>
            ) : null}
            <Button
              variant="ghost"
              size="sm"
              className="ml-auto border border-[var(--color-border)]"
              onClick={() => void list.fetchNextPage()}
              disabled={!list.hasNextPage || list.isFetchingNextPage}
            >
              {list.isFetchNextPageError
                ? t("retry")
                : t("loadMore", { count: LOAD_MORE_COUNT })}
            </Button>
          </div>
        </Card>

        <div className="flex min-w-0 flex-col gap-4">
          <Card
            title={t("activeAdminsTitle")}
            description={t("activeAdminsSubtitle")}
          >
            <QueryBoundary query={activeAdmins}>
              {(admins) => (
                <div className="flex flex-col gap-2.5">
                  {admins.map((admin) => (
                    <SettingRow
                      key={admin.id ?? admin.name}
                      label={admin.name}
                      description={admin.meta}
                      leading={
                        <span
                          aria-hidden="true"
                          className="glass-surface flex h-[26px] w-[26px] items-center justify-center rounded-full border border-[var(--color-border)] text-[10.5px] font-semibold"
                        >
                          {initialsOf(admin.name)}
                        </span>
                      }
                    >
                      <span
                        className="font-mono text-xs font-semibold whitespace-nowrap"
                        style={
                          admin.colorVar
                            ? { color: textTone(admin.colorVar) }
                            : undefined
                        }
                      >
                        {admin.lastActionAt}
                      </span>
                    </SettingRow>
                  ))}
                </div>
              )}
            </QueryBoundary>
          </Card>

          <Card title={t("breakdownTitle")}>
            <QueryBoundary query={breakdown}>
              {(items) => (
                <RankedProgressList
                  numbered={false}
                  items={items.map((item) => ({
                    label: t(`category.${item.category}`),
                    value: item.count,
                    colorVar: CATEGORY_COLOR_VAR[item.category],
                  }))}
                  viewAllHref="/admin/queue"
                  viewAllLabel={t("infraNote")}
                />
              )}
            </QueryBoundary>
          </Card>
        </div>
      </div>

      {/* BD ADM0403 area G: read-only; the number is edited in the Lưu trữ dialog. */}
      {settings ? (
        <p className="mt-4 text-[12.5px] text-[var(--color-text-subtle)]">
          {t("retention.footer", { days: String(settings.retentionDays) })}
        </p>
      ) : null}

      {settings ? (
        <ParamsDialog
          open={editingRetention}
          onClose={() => setEditingRetention(false)}
          title={t("retention.title")}
          values={{
            retentionDays: String(settings.retentionDays),
            expiry: settings.expiry,
          }}
          fields={[
            {
              type: "number",
              key: "retentionDays",
              label: t("retention.days"),
              unit: t("retention.daysUnit"),
              min: 1,
            },
            {
              type: "select",
              key: "expiry",
              label: t("retention.policy"),
              hint: t("retention.hint"),
              options: [
                { value: "delete", label: t("retention.policyDelete") },
                { value: "archive", label: t("retention.policyArchive") },
              ],
            },
          ]}
          confirmSave={(draft) => {
            // Shortening the period, or switching to "delete", is what makes old rows disappear (Q7a).
            const warnings: string[] = [];
            if (Number(draft.retentionDays) < settings.retentionDays) {
              warnings.push(
                t("retention.confirmShorter", {
                  days: draft.retentionDays ?? "",
                }),
              );
            }
            if (draft.expiry === "delete" && settings.expiry !== "delete") {
              warnings.push(t("retention.confirmDelete"));
            }
            return warnings.length ? warnings.join(" ") : undefined;
          }}
          onSave={async (values) => {
            try {
              await saveRetention.mutateAsync({
                retentionDays: Number(values.retentionDays),
                expiry: values.expiry as ExpiryPolicy,
              });
            } catch (error) {
              // The draft stays in the dialog; the stored value on the page does not change.
              toast.error(toMessage(error));
              throw error;
            }
          }}
          labels={{
            save: t("retention.save"),
            cancel: t("retention.cancel"),
            saved: t("retention.saved"),
            errorMin: (min) => t("retention.errorMin", { min }),
            confirm: t("retention.confirm"),
            back: t("retention.back"),
          }}
        />
      ) : null}

      {/* Mounted only while open so the dialog starts from the filters on screen now (Q8). */}
      {exporting && settings ? (
        <ExportLogDialog
          open
          onClose={() => setExporting(false)}
          retentionDays={settings.retentionDays}
          scope={{
            query: query.trim(),
            category,
            categoryLabel:
              category === "all" ? null : t(`category.${category}`),
            from,
            to,
          }}
        />
      ) : null}
    </div>
  );
}

/**
 * One timeline entry. The left rule is coloured per category (dc.html:194) — it repeats the badge's
 * meaning visually, so it is aria-hidden and the badge carries the accessible text.
 */
function EventRow({
  event,
  message,
  categoryLabel,
  reasonLabel,
}: {
  event: AuditEvent;
  message: string;
  categoryLabel: string;
  reasonLabel: string;
}) {
  return (
    <li
      className="grid grid-cols-[66px_96px_minmax(180px,1fr)] items-start gap-3 rounded-xl border-l-2 px-2.5 py-2.5 hover:bg-[var(--color-row-hover)]"
      style={{ borderLeftColor: `var(${CATEGORY_COLOR_VAR[event.category]})` }}
    >
      <span className="pt-px font-mono text-xs text-[var(--color-text-subtle)]">
        {event.time}
        <span className="block text-[10.5px]">{formatDay(event.date)}</span>
      </span>
      <Badge
        variant={CATEGORY_VARIANT[event.category]}
        className="w-full justify-center font-mono"
      >
        {categoryLabel}
      </Badge>
      <span className="min-w-0">
        <span className="block text-[13px] font-semibold text-pretty">
          {message}
        </span>
        {event.reason ? (
          <span className="mt-0.5 block text-[12.5px] text-[var(--color-text-muted)]">
            {reasonLabel}: {event.reason}
          </span>
        ) : null}
        <span className="mt-0.5 flex flex-wrap items-center gap-2 font-mono text-[11.5px] text-[var(--color-text-subtle)]">
          <span>{event.service}</span>
          <span aria-hidden="true">·</span>
          <span>{event.actor}</span>
          <span aria-hidden="true">·</span>
          <span>{event.shortId}</span>
        </span>
      </span>
    </li>
  );
}
