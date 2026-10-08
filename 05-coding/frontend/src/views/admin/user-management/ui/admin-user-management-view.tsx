// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Layout follows 09-layoutBase/Admin - Người dùng.dc.html: sticky header with the "Thêm tài khoản"
// CTA (:148-159), stat cards (:161-173), the account table card (:175-236), then two panels side by
// side (:238-274).
//
// Divergences, each backed by a document:
//
// 1. Bulk lock goes through LockAccountsDialog. BD section 3 defines a `bulk-action-confirming` state
//    for it — "hành động phá huỷ khả năng đăng nhập" — and flags it [SoT: Suy luận] because the
//    static mockup never drew a dialog. The mockup's button clears the selection instead of locking,
//    which is plainly a stand-in.
// 2. Owner instruction 2026-10-05: bulk selection now carries only the status actions. "Đặt lại mật
//    khẩu" and "Đổi vai trò" were dropped — resetting a stranger's password is per-person (F1-13 lists
//    it as an account-troubleshooting act, and OAuth-only accounts cannot take a password at all,
//    BD section 9 NO 10), and demoting a batch of instructors to one target role is a mistake with no
//    undo. Locking is the one action that is genuinely the same decision for everyone in a selection,
//    because it is what incident response looks like. Both survivors came out of BD section 4 Q2,
//    which was still open.
// 3. Locking a selection now also takes a reason and can mail the affected users. F1-13 never asked
//    for either — this opens a new `Fx-nn` code and is NOT yet in 01-rd/req/identity.md. See
//    lock-accounts-dialog.tsx.
//
// Known gap, deliberately left visible rather than papered over: the "Hoạt động" column and the
// "Đang hoạt động 24 giờ" stat HAVE NO DATA SOURCE. `database/identity.md` has no last-seen column.
// This is level B1 in 07-review/bd_screens_admin_open_questions_260913.md, which proposes
// `users.last_active_at`. Rendered from the mock as a plain label so nobody mistakes it for a
// settled contract.
"use client";

import { useMemo, useState } from "react";
import { KeyRound, Lock, LockOpen, UserCog } from "lucide-react";
import {
  fetchAdminUserPage,
  initialsOf,
  type AdminUser,
  type AdminUserRole,
  type AdminUserStatus,
} from "../api";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import { checkLock, checkRoleChange, partitionByStatus } from "../model/guards";
import { AddAccountDialog } from "./add-account-dialog";
import { ChangeRoleDialog } from "./change-role-dialog";
import { LockAccountsDialog } from "./lock-accounts-dialog";
import {
  Badge,
  BulkActionBar,
  Button,
  Card,
  ConfirmDialog,
  DataTable,
  PageHeader,
  Pagination,
  RankedProgressList,
  FilterBar,
  FilterMenu,
  IconAction,
  SettingRow,
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
  const [accounts, setAccounts] = useState(page.users);
  const [adding, setAdding] = useState(false);
  const [query, setQuery] = useState("");
  const [role, setRole] = useState<RoleFilter>("all");
  const [status, setStatus] = useState<StatusFilter>("all");
  const [selected, setSelected] = useState<ReadonlySet<string>>(new Set());
  // The lock and unlock dialogs act on whichever rows opened them: the bulk bar passes the selection,
  // a row action passes that one account. `null` = closed.
  const [lockRequest, setLockRequest] = useState<{
    targets: AdminUser[];
    skipped: number;
    includesSelf: boolean;
  } | null>(null);
  const [unlockTargets, setUnlockTargets] = useState<AdminUser[] | null>(null);
  const [roleTarget, setRoleTarget] = useState<AdminUser | null>(null);
  const [resetTarget, setResetTarget] = useState<AdminUser | null>(null);

  // Role, status and free text combine with AND — same as the mockup's own filter (dc.html:449-453).
  const users = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return accounts.filter(
      (user) =>
        (role === "all" || user.role === role) &&
        (status === "all" || user.status === status) &&
        (!needle ||
          user.name.toLowerCase().includes(needle) ||
          user.email.toLowerCase().includes(needle)),
    );
  }, [accounts, query, role, status]);

  // A selection can mix active and locked rows, so each button acts on its own subset and reports
  // what it would skip rather than firing a call that changes nothing.
  const { lockable, unlockable } = partitionByStatus(accounts, selected);

  // Every filter change drops the selection (BD section 6, Khu vực C NO 2: "Bỏ toàn bộ lựa chọn khi
  // đổi trang, đổi bộ lọc"). Without this the admin filters down to one row, ticks it, filters back
  // and the bar still reads "9 selected" — and "Khóa tài khoản" would then hit nine people they never
  // looked at. Cleared in each setter rather than in an effect: an effect here would run one render
  // late, so the bar would briefly show a stale count against the new filter.
  function changeQuery(next: string) {
    setQuery(next);
    setSelected(new Set());
  }

  function changeRole(next: RoleFilter) {
    setRole(next);
    setSelected(new Set());
  }

  function changeStatus(next: StatusFilter) {
    setStatus(next);
    setSelected(new Set());
  }

  function toggleRow(key: string) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  }

  function requestLock(targets: AdminUser[], skipped: number) {
    const check = checkLock(
      accounts,
      new Set(targets.map((user) => user.email)),
      page.currentUserEmail,
    );
    // "At least one active ADMIN always remains" is a hard block; locking yourself only warns.
    if (check.blockedLastAdmin) toast.warning(t("lockBlockedBody"));
    else setLockRequest({ targets, skipped, includesSelf: check.includesSelf });
  }

  function applyLock(reason: string, notify: boolean) {
    // No endpoint yet — the rows flip locally and the selection clears.
    const target = new Set(
      (lockRequest?.targets ?? []).map((user) => user.email),
    );
    setAccounts((previous) =>
      previous.map((account) =>
        target.has(account.email) ? { ...account, status: "locked" } : account,
      ),
    );
    toast.success(
      notify
        ? t("lockDoneNotified", { count: target.size, reason })
        : t("lockDone", { count: target.size, reason }),
    );
    setSelected(new Set());
  }

  function applyUnlock() {
    const target = new Set((unlockTargets ?? []).map((user) => user.email));
    setAccounts((previous) =>
      previous.map((account) =>
        target.has(account.email) ? { ...account, status: "active" } : account,
      ),
    );
    toast.success(t("unlockDone", { count: target.size }));
    setSelected(new Set());
    setUnlockTargets(null);
  }

  function applyRoleChange(user: AdminUser, nextRole: AdminUserRole) {
    const check = checkRoleChange(
      accounts,
      user.email,
      nextRole,
      page.currentUserEmail,
    );
    if (check.blockedLastAdmin) {
      toast.warning(t("roleBlockedBody"));
      return;
    }
    setAccounts((previous) =>
      previous.map((account) =>
        account.email === user.email ? { ...account, role: nextRole } : account,
      ),
    );
    toast.success(
      t("roleDone", { name: user.name, role: t(`role.${nextRole}`) }),
    );
  }

  function applyResetPassword() {
    if (resetTarget)
      toast.success(t("resetDone", { email: resetTarget.email }));
    setResetTarget(null);
  }

  function toggleAll(selectAll: boolean) {
    setSelected(
      selectAll ? new Set(users.map((user) => user.email)) : new Set(),
    );
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
      render: (user) => (
        <Badge variant={ROLE_VARIANT[user.role]}>
          {t(`role.${user.role}`)}
        </Badge>
      ),
    },
    {
      key: "solved",
      header: t("columnSolved"),
      width: "84px",
      align: "right",
      render: (user) => (
        <span className="font-mono font-semibold">{user.solvedCount}</span>
      ),
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
      width: "132px",
      align: "right",
      render: (user) => (
        <span
          className="flex items-center justify-end gap-1.5 text-[12.5px] font-semibold whitespace-nowrap"
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
    {
      // Per-account actions: the touchpoint ADM0201 Q7 left open once reset-password and change-role
      // left the bulk bar. Lock/unlock sit here too so a single account never needs a selection.
      key: "actions",
      header: t("columnActions"),
      width: "132px",
      align: "right",
      render: (user) => (
        <span className="flex items-center justify-end gap-1.5">
          <IconAction
            icon={UserCog}
            label={t("actionChangeRole")}
            ariaLabel={t("actionChangeRoleFor", { name: user.name })}
            onClick={() => setRoleTarget(user)}
          />
          <IconAction
            icon={KeyRound}
            label={t("actionResetPassword")}
            ariaLabel={t("actionResetPasswordFor", { name: user.name })}
            onClick={() => setResetTarget(user)}
          />
          {user.status === "locked" ? (
            <IconAction
              icon={LockOpen}
              label={t("actionUnlock")}
              ariaLabel={t("actionUnlockFor", { name: user.name })}
              onClick={() => setUnlockTargets([user])}
            />
          ) : (
            <IconAction
              icon={Lock}
              tone="danger"
              label={t("actionLock")}
              ariaLabel={t("actionLockFor", { name: user.name })}
              onClick={() => requestLock([user], 0)}
            />
          )}
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
          <Button variant="cta" size="sm" onClick={() => setAdding(true)}>
            {t("addAccount")}
          </Button>
        }
      />

      <Card className="mb-4 min-w-0 px-[18px] py-4">
        <FilterBar
          search={{
            label: t("searchLabel"),
            placeholder: t("searchPlaceholder"),
            value: query,
            onChange: changeQuery,
          }}
          resultCount={t("resultCount", {
            shown: users.length,
            total: accounts.length,
          })}
        >
          <FilterMenu
            label={t("roleFilterLabel")}
            value={role}
            onValueChange={changeRole}
            options={[
              { value: "all", label: t("filterAll") },
              { value: "student", label: t("role.student") },
              { value: "instructor", label: t("role.instructor") },
              { value: "admin", label: t("role.admin") },
            ]}
          />
          <FilterMenu
            label={t("statusFilterLabel")}
            value={status}
            onValueChange={changeStatus}
            options={[
              { value: "all", label: t("filterAll") },
              { value: "active", label: t("status.active") },
              { value: "pending", label: t("status.pending") },
              { value: "locked", label: t("status.locked") },
            ]}
          />
        </FilterBar>

        <BulkActionBar
          count={selected.size}
          label={t("selectionLabel", { count: selected.size })}
        >
          {/* Disabled rather than hidden when the selection has nothing to change: a selection of only
              locked rows still needs the unlock button, and one of only active rows still needs lock,
              so hiding either would make the bar flicker as the admin ticks across the page. */}
          <Button
            variant="ghost"
            size="sm"
            className="border border-[var(--color-border)] text-[var(--color-admin-negative-text)]"
            onClick={() => requestLock(lockable, unlockable.length)}
            disabled={lockable.length === 0}
          >
            {t("bulkLock")}
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="border border-[var(--color-border)]"
            onClick={() => setUnlockTargets(unlockable)}
            disabled={unlockable.length === 0}
          >
            {t("bulkUnlock")}
          </Button>
        </BulkActionBar>

        <DataTable
          caption={t("tableCaption")}
          columns={columns}
          rows={users}
          rowKey={(user) => user.email}
          emptyMessage={t("emptyFiltered")}
          minWidth={910}
          selection={{
            selectedKeys: selected,
            onToggleRow: toggleRow,
            onToggleAll: toggleAll,
            selectAllLabel: t("selectAll", { count: users.length }),
            rowLabel: (user) => t("selectRow", { name: user.name }),
          }}
        />

        <Pagination
          page={1}
          pageSize={PAGE_SIZE}
          total={page.totalUsers}
          onPageChange={() => {}}
          summary={t("pageLabel", {
            page: 1,
            totalPages: page.totalPages,
            shown: users.length,
          })}
          previousLabel={t("previous")}
          nextLabel={t("next")}
        />
      </Card>

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
        <Card
          title={t("roleDistributionTitle")}
          description={t("roleDistributionSubtitle")}
        >
          <RankedProgressList
            numbered={false}
            items={page.roleDistribution.map((item) => ({
              label:
                item.role === "deactivated"
                  ? t("role.deactivated")
                  : t(`role.${item.role}`),
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

      <AddAccountDialog
        open={adding}
        onClose={() => setAdding(false)}
        existingEmails={new Set(accounts.map((account) => account.email))}
        onCreate={(user) => setAccounts((previous) => [user, ...previous])}
      />

      <LockAccountsDialog
        open={lockRequest !== null}
        onClose={() => setLockRequest(null)}
        targets={lockRequest?.targets ?? []}
        skippedCount={lockRequest?.skipped ?? 0}
        includesSelf={lockRequest?.includesSelf ?? false}
        onConfirm={applyLock}
      />

      <ChangeRoleDialog
        user={roleTarget}
        isSelf={roleTarget?.email === page.currentUserEmail}
        onClose={() => setRoleTarget(null)}
        onConfirm={applyRoleChange}
      />

      {/* Reset is per person (RD ADM0201 Q7): the temporary password goes to that one inbox, so the
          dialog names the address instead of a count. */}
      <ConfirmDialog
        open={resetTarget !== null}
        onClose={() => setResetTarget(null)}
        onConfirm={applyResetPassword}
        title={t("resetTitle", { name: resetTarget?.name ?? "" })}
        confirmLabel={t("resetAction")}
        cancelLabel={t("cancel")}
      >
        <p>{t("resetBody", { email: resetTarget?.email ?? "" })}</p>
      </ConfirmDialog>

      {/* Unlock is not destructive — it restores access — so it needs a plain confirmation and no
          reason field. The names are listed for the same reason as on the lock side: confirming
          "yes, 3 accounts" says nothing about which three. */}
      <ConfirmDialog
        open={unlockTargets !== null}
        onClose={() => setUnlockTargets(null)}
        onConfirm={applyUnlock}
        title={t("confirmUnlockTitle", { count: unlockTargets?.length ?? 0 })}
        confirmLabel={t("confirmUnlockAction")}
        cancelLabel={t("cancel")}
      >
        <p>{t("confirmUnlockBody", { count: unlockTargets?.length ?? 0 })}</p>
        <ul className="mt-2 max-h-40 overflow-y-auto rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-2 text-[12.5px]">
          {(unlockTargets ?? []).slice(0, 8).map((user) => (
            <li
              key={user.email}
              className="flex items-baseline justify-between gap-3 py-0.5"
            >
              <span className="truncate font-semibold">{user.name}</span>
              <span className="shrink-0 font-mono text-[11px] text-[var(--color-text-subtle)]">
                {user.email}
              </span>
            </li>
          ))}
          {(unlockTargets?.length ?? 0) > 8 ? (
            <li className="py-0.5 text-[var(--color-text-muted)]">
              {t("lockDialog.moreCount", {
                count: (unlockTargets?.length ?? 0) - 8,
              })}
            </li>
          ) : null}
        </ul>
      </ConfirmDialog>
    </div>
  );
}
