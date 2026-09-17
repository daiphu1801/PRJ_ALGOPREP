// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Layout follows 09-layoutBase/Admin - Nhật ký hệ thống.dc.html: sticky header with a live toggle
// and an export CTA (:149-163), a row of stat cards (:165-177), then a two-column grid
// 1fr / minmax(290px,0.42fr) (:179) — event list left (:181-214), two side panels right (:216-246).
//
// Everything belonging to the removed rejudge feature is stripped, per
// 02-bd/screens/admin/admin_system_log.md section 0 and DEC-2026-0828-remove-rejudge-scope: the
// fourth "Phiên chấm lại đã chạy" stat, the `#RJ-0139` sample event, and the "Chấm lại" category.
// The mockup still carries all three. The data lives in entities/audit-log, already trimmed.
//
// The event list is deliberately NOT a DataTable: BD section 2 specifies a timeline whose rows wrap
// onto two lines with a category-coloured left rule, plus "Tải thêm 50 dòng" instead of numbered
// paging — a column grid would fight that shape rather than help it.
"use client";

import { useMemo, useState } from "react";
import { Search } from "lucide-react";
import {
  AUDIT_CATEGORIES,
  fetchAuditLogPage,
  initialsOf,
  type AuditCategory,
  type AuditEvent,
} from "@/entities/audit-log";
import { useT } from "@/shared/i18n";
import {
  Badge,
  Button,
  Card,
  EmptyState,
  PageHeader,
  RankedProgressList,
  SegmentedTabs,
  SettingRow,
  StatCard,
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

export function AdminSystemLogView() {
  const t = useT("adminSystemLog");
  const [page] = useState(fetchAuditLogPage);
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState<CategoryFilter>("all");
  const [live, setLive] = useState(true);

  // Category and free text combine with AND, matching the prototype's own filter (dc.html:414-418).
  // Search covers service, actor and event id — not the message — because the BD rules out a
  // full-text scan over the message column on a large table (BD section 2, AuditLogSearchBar).
  const events = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return page.events.filter((event) => {
      const categoryOk = category === "all" || event.category === category;
      const textOk =
        !needle ||
        `${event.service} ${event.actor} ${event.id}`.toLowerCase().includes(needle);
      return categoryOk && textOk;
    });
  }, [page.events, query, category]);

  const categoryOptions = [
    { value: "all" as const, label: t("categoryAll") },
    ...AUDIT_CATEGORIES.map((key) => ({ value: key, label: t(`category.${key}`) })),
  ];

  return (
    <div>
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        actions={
          <>
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
                  live ? "animate-pulse bg-[var(--color-success)]" : "bg-[var(--color-text-subtle)]"
                }`}
              />
              {live ? t("liveOn") : t("liveOff")}
            </Button>
            <Button variant="cta" size="sm">
              {t("export")}
            </Button>
          </>
        }
      />

      <div className="mb-4 grid gap-3.5 [grid-template-columns:repeat(auto-fit,minmax(190px,1fr))]">
        {page.stats.map((stat) => (
          <StatCard
            key={stat.key}
            label={t(`stat.${stat.key}.label`)}
            value={stat.value}
            delta={stat.delta}
            meta={t(`stat.${stat.key}.meta`)}
            valueColorVar={stat.colorVar}
          />
        ))}
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(290px,0.42fr)]">
        <Card className="min-w-0 px-[18px] py-4">
          <div className="mb-3.5 flex flex-wrap items-center gap-2.5">
            <TextField
              label={t("searchLabel")}
              hideLabel
              leadingIcon={<Search className="h-3.5 w-3.5" />}
              placeholder={t("searchPlaceholder")}
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              wrapperClassName="min-w-[200px] flex-1"
            />
            <SegmentedTabs
              label={t("categoryFilterLabel")}
              options={categoryOptions}
              value={category}
              onValueChange={setCategory}
            />
            <span className="ml-auto text-[12.5px] whitespace-nowrap text-[var(--color-text-muted)]">
              {t("resultCount", { shown: events.length, total: page.events.length })}
            </span>
          </div>

          {events.length === 0 ? (
            <EmptyState>
              <span className="flex flex-col items-center gap-2">
                {t("emptyFiltered")}
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    setQuery("");
                    setCategory("all");
                  }}
                >
                  {t("clearFilters")}
                </Button>
              </span>
            </EmptyState>
          ) : (
            <ul aria-label={t("eventListLabel")} className="flex flex-col gap-0.5">
              {events.map((event) => (
                <EventRow key={event.id} event={event} categoryLabel={t(`category.${event.category}`)} />
              ))}
            </ul>
          )}

          <div className="mt-1.5 flex items-center gap-3 border-t border-[var(--color-border)] px-2.5 pt-3">
            <span className="text-[12.5px] text-[var(--color-text-muted)]">
              {t("pageLabel", { shown: events.length, total: page.totalEvents.toLocaleString("vi-VN") })}
            </span>
            <Button variant="ghost" size="sm" className="ml-auto border border-[var(--color-border)]">
              {t("loadMore", { count: 50 })}
            </Button>
          </div>
        </Card>

        <div className="flex min-w-0 flex-col gap-4">
          <Card title={t("activeAdminsTitle")} description={t("activeAdminsSubtitle")}>
            <div className="flex flex-col gap-2.5">
              {page.activeAdmins.map((admin) => (
                <SettingRow
                  key={admin.name}
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
                    style={admin.colorVar ? { color: `var(${admin.colorVar})` } : undefined}
                  >
                    {admin.lastActionAt}
                  </span>
                </SettingRow>
              ))}
            </div>
          </Card>

          <Card title={t("breakdownTitle")}>
            <RankedProgressList
              numbered={false}
              items={page.breakdown.map((item) => ({
                label: t(`category.${item.category}`),
                value: item.count,
                colorVar: CATEGORY_COLOR_VAR[item.category],
              }))}
              footnote={t("infraNote")}
            />
          </Card>
        </div>
      </div>
    </div>
  );
}

/**
 * One timeline entry. The left rule is coloured per category (dc.html:194) — it repeats the badge's
 * meaning visually, so it is aria-hidden and the badge carries the accessible text.
 */
function EventRow({ event, categoryLabel }: { event: AuditEvent; categoryLabel: string }) {
  return (
    <li
      className="grid grid-cols-[66px_96px_minmax(180px,1fr)] items-start gap-3 rounded-xl border-l-2 px-2.5 py-2.5 hover:bg-[var(--color-row-hover)]"
      style={{ borderLeftColor: `var(${CATEGORY_COLOR_VAR[event.category]})` }}
    >
      <span className="pt-px font-mono text-xs text-[var(--color-text-subtle)]">{event.time}</span>
      <Badge variant={CATEGORY_VARIANT[event.category]} className="w-full justify-center font-mono">
        {categoryLabel}
      </Badge>
      <span className="min-w-0">
        <span className="block text-[13px] font-semibold text-pretty">{event.message}</span>
        <span className="mt-0.5 flex flex-wrap items-center gap-2 font-mono text-[11.5px] text-[var(--color-text-subtle)]">
          <span>{event.service}</span>
          <span aria-hidden="true">·</span>
          <span>{event.actor}</span>
          <span aria-hidden="true">·</span>
          <span>{event.id}</span>
        </span>
      </span>
    </li>
  );
}
