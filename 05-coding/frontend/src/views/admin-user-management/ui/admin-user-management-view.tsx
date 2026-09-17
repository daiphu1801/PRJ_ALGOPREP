// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Layout follows 09-layoutBase/Admin - Người dùng.dc.html: sticky header with the "Thêm tài khoản"
// CTA (:148-159), stat cards (:161-173), the account table card (:175-236), then two panels side by
// side (:238-274).
//
// Divergences, each backed by a document:
//
// 1. Bulk lock goes through ConfirmDialog. BD section 3 defines a `bulk-action-confirming` state for
//    it — "hành động phá huỷ khả năng đăng nhập" — and flags it [SoT: Suy luận] because the static
//    mockup never drew a dialog. The mockup's button clears the selection instead of locking, which
//    is plainly a stand-in.
// 2. The selection bar's lock button is styled as destructive; the other two are not.
//
// Known gap, deliberately left visible rather than papered over: the "Hoạt động" column and the
// "Đang hoạt động 24 giờ" stat HAVE NO DATA SOURCE. `database/identity.md` has no last-seen column.
// This is level B1 in 07-review/bd_screens_admin_open_questions_260913.md, which proposes
// `users.last_active_at`. Rendered from the mock as a plain label so nobody mistakes it for a
// settled contract.
"use client";

import { useMemo, useState } from "react";
import { Search } from "lucide-react";
import {
  fetchAdminUserPage,
  initialsOf,
  type AdminUser,
  type AdminUserRole,
  type AdminUserStatus,
} from "@/entities/admin-user";
import { useT } from "@/shared/i18n";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  DataTable,
  PageHeader,
  Pagination,
  RankedProgressList,
  SegmentedTabs,
  SettingRow,
  StatCard,
  TextField,
  type BadgeVariant,
  type DataTableColumn,
} from "@/shared/ui";

// dc.html:430-434.
const ROLE_VARIANT: Record<AdminUserRole, BadgeVariant> = {
  student: "blue",
  instructor: "purple",
  admin: "success",
};

const STATUS_COLOR_VAR: Record<AdminUserStatus, string> = {
  active: "--color-success",
  pending: "--color-admin-warn",
  locked: "--color-admin-negative",
};

const ROLE_BAR_COLOR_VAR: Record<AdminUserRole | "deactivated", string> = {
  student: "--color-admin-teal",
  instructor: "--color-admin-warn",
  admin: "--color-success",
  deactivated: "--color-admin-negative",
};

type RoleFilter = AdminUserRole | "all";
type StatusFilter = AdminUserStatus | "all";

const PAGE_SIZE = 20;

export function AdminUserManagementView() {
  const t = useT("adminUserManagement");
  const [page] = useState(fetchAdminUserPage);
  const [query, setQuery] = useState("");
  const [role, setRole] = useState<RoleFilter>("all");
  const [status, setStatus] = useState<StatusFilter>("all");
  const [selected, setSelected] = useState<ReadonlySet<string>>(new Set());
  const [confirmingLock, setConfirmingLock] = useState(false);

  // Role, status and free text combine with AND — same as the mockup's own filter (dc.html:449-453).
  const users = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return page.users.filter(
      (user) =>
        (role === "all" || user.role === role) &&
        (status === "all" || user.status === status) &&
        (!needle ||
          user.name.toLowerCase().includes(needle) ||
          user.email.toLowerCase().includes(needle)),
    );
  }, [page.users, query, role, status]);

  function toggleRow(key: string) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  }

  function toggleAll(selectAll: boolean) {
    setSelected(selectAll ? new Set(users.map((user) => user.email)) : new Set());
  }

  const columns: DataTableColumn<AdminUser>[] = [
    {
      key: "user",
      header: t("columnUser"),
      render: (user) => (
        <div className="flex min-w-0 items-center gap-2.5">
          <span
            aria-hidden="true"
            className="glass-surface flex h-[30px] w-[30px] shrink-0 items-center justify-center rounded-full border border-[var(--color-border)] text-[11px] font-semibold"
          >
            {initialsOf(user.name)}
          </span>
          <span className="min-w-0">
            <span className="block truncate font-semibold">{user.name}</span>
            <span className="block truncate font-mono text-[11px] text-[var(--color-text-subtle)]">
              {user.email}
            </span>
          </span>
        </div>
      ),
    },
    {
      key: "role",
      header: t("columnRole"),
      width: "116px",
      render: (user) => <Badge variant={ROLE_VARIANT[user.role]}>{t(`role.${user.role}`)}</Badge>,
    },
    {
      key: "solved",
      header: t("columnSolved"),
      width: "84px",
      align: "right",
      render: (user) => <span className="font-mono font-semibold">{user.solvedCount}</span>,
    },
    {
      key: "submissions",
      header: t("columnSubmissions"),
      width: "92px",
      align: "right",
      render: (user) => (
        <span className="font-mono text-[var(--color-text-muted)]">
          {user.submissionCount.toLocaleString("vi-VN")}
        </span>
      ),
    },
    {
      key: "lastActive",
      header: t("columnLastActive"),
      width: "116px",
      render: (user) => (
        <span className="block truncate text-[12.5px] text-[var(--color-text-muted)]">
          {user.lastActiveLabel}
        </span>
      ),
    },
    {
      key: "status",
      header: t("columnStatus"),
      width: "104px",
      align: "right",
      render: (user) => (
        <span
          className="flex items-center justify-end gap-1.5 text-[12.5px] font-semibold"
          style={{ color: `var(${STATUS_COLOR_VAR[user.status]})` }}
        >
          <span
            aria-hidden="true"
            className="inline-block h-1.5 w-1.5 rounded-full"
            style={{ background: `var(${STATUS_COLOR_VAR[user.status]})` }}
          />
          {t(`status.${user.status}`)}
        </span>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        actions={
          <Button variant="cta" size="sm">
            {t("addAccount")}
          </Button>
        }
      />

      <div className="mb-4 grid gap-3.5 [grid-template-columns:repeat(auto-fit,minmax(200px,1fr))]">
        {page.stats.map((stat) => (
          <StatCard
            key={stat.key}
            label={t(`stat.${stat.key}.label`)}
            value={stat.value}
            delta={<span style={{ color: `var(${stat.deltaColorVar})` }}>{stat.delta}</span>}
            meta={t(`stat.${stat.key}.meta`)}
          />
        ))}
      </div>

      <Card className="mb-4 min-w-0 px-[18px] py-4">
        <div className="mb-3.5 flex flex-wrap items-center gap-2.5">
          <TextField
            label={t("searchLabel")}
            hideLabel
            leadingIcon={<Search className="h-3.5 w-3.5" />}
            placeholder={t("searchPlaceholder")}
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            wrapperClassName="min-w-[220px] flex-1"
          />
          <SegmentedTabs
            label={t("roleFilterLabel")}
            value={role}
            onValueChange={setRole}
            options={[
              { value: "all", label: t("filterAll") },
              { value: "student", label: t("role.student") },
              { value: "instructor", label: t("role.instructor") },
              { value: "admin", label: t("role.admin") },
            ]}
          />
          <SegmentedTabs
            label={t("statusFilterLabel")}
            value={status}
            onValueChange={setStatus}
            options={[
              { value: "all", label: t("filterAll") },
              { value: "active", label: t("status.active") },
              { value: "locked", label: t("status.locked") },
            ]}
          />
          <span className="ml-auto text-[12.5px] whitespace-nowrap text-[var(--color-text-muted)]">
            {t("resultCount", { shown: users.length, total: page.users.length })}
          </span>
        </div>

        {selected.size > 0 ? (
          <div className="mb-3 flex flex-wrap items-center gap-2.5 rounded-2xl border border-[var(--admin-active-border)] bg-[image:var(--color-row-selected)] px-3.5 py-2.5">
            <span className="text-[13px] font-semibold">
              {t("selectionLabel", { count: selected.size })}
            </span>
            <span className="ml-auto flex flex-wrap gap-2">
              <Button variant="ghost" size="sm" className="border border-[var(--color-border)]">
                {t("bulkResetPassword")}
              </Button>
              <Button variant="ghost" size="sm" className="border border-[var(--color-border)]">
                {t("bulkChangeRole")}
              </Button>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setConfirmingLock(true)}
                className="text-[var(--color-admin-negative)]"
              >
                {t("bulkLock")}
              </Button>
            </span>
          </div>
        ) : null}

        <DataTable
          caption={t("tableCaption")}
          columns={columns}
          rows={users}
          rowKey={(user) => user.email}
          emptyMessage={t("emptyFiltered")}
          minWidth={760}
          selection={{
            selectedKeys: selected,
            onToggleRow: toggleRow,
            onToggleAll: toggleAll,
            selectAllLabel: t("selectAll"),
            rowLabel: (user) => t("selectRow", { name: user.name }),
          }}
        />

        <Pagination
          page={1}
          pageSize={PAGE_SIZE}
          total={page.totalUsers}
          onPageChange={() => {}}
          summary={t("pageLabel", { page: 1, totalPages: page.totalPages, shown: users.length })}
          previousLabel={t("previous")}
          nextLabel={t("next")}
        />
      </Card>

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
        <Card title={t("roleDistributionTitle")} description={t("roleDistributionSubtitle")}>
          <RankedProgressList
            numbered={false}
            items={page.roleDistribution.map((item) => ({
              label: item.role === "deactivated" ? t("role.deactivated") : t(`role.${item.role}`),
              value: item.percent,
              colorVar: ROLE_BAR_COLOR_VAR[item.role],
            }))}
          />
        </Card>

        <Card title={t("pendingTitle")}>
          <div className="flex flex-col gap-2.5">
            {page.pendingTasks.map((task) => (
              <SettingRow
                key={task.key}
                label={t(`pending.${task.key}.label`)}
                description={t(`pending.${task.key}.meta`)}
              >
                <span
                  className="font-mono text-[13px] font-semibold"
                  style={{ color: `var(${task.colorVar})` }}
                >
                  {task.count}
                </span>
              </SettingRow>
            ))}
          </div>
        </Card>
      </div>

      <ConfirmDialog
        open={confirmingLock}
        onClose={() => setConfirmingLock(false)}
        onConfirm={() => {
          // No endpoint yet — clearing the selection is the visible outcome of a successful lock.
          setSelected(new Set());
          setConfirmingLock(false);
        }}
        title={t("confirmLockTitle", { count: selected.size })}
        confirmLabel={t("confirmLockAction")}
        cancelLabel={t("cancel")}
        destructive
      >
        {t("confirmLockBody", { count: selected.size })}
      </ConfirmDialog>
    </div>
  );
}
