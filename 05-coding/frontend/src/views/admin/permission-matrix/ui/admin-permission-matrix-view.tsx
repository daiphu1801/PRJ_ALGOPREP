// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Layout follows 09-layoutBase/Admin - Ma trận phân quyền.dc.html: sticky header (:149-159), a
// standing note about what the matrix does and does not govern (:161-163), then one card holding
// the role switcher, the create/delete role controls and the Function x Action grid (:165-223).
//
// Divergences:
//
// 1. TEN functions, not eleven. `REJUDGE_MANAGEMENT` is gone with the rest of the rejudge feature
//    (DEC-2026-0828-remove-rejudge-scope); 01-rd/req/identity.md F1-12 already lists ten, so the
//    mockup is the stale copy here.
// 2. Toggling a cell applies immediately and there is NO save button. F1-10 says the change "có hiệu
//    lực ngay", which the mockup's own subtitle repeats; ADM0202 Q1 was closed 2026-10-08 on that
//    reading (owner), so the mockup's header button is dropped and Q5 (confirm on leaving) goes away.
// 3. Two guards from BD Sheet 9: turning off a cell of the role the acting admin holds asks first
//    (NO 8, warning), and the last ADMIN-category PERMISSION_MATRIX:UPDATE cannot be removed (NO 9,
//    hard block, ADM0202 Q4). A new role must have a unique name of at most 50 characters (NO 3-4).
//
// The grid is a DataTable, not a bespoke component: it is a Function column plus one column per
// Action, which is exactly a column spec. The cells happen to be checkboxes.
"use client";

import { useState } from "react";
import {
  ACTION_KEYS,
  FUNCTION_KEYS,
  fetchPermissionMatrix,
  grantsFor,
  type ActionKey,
  type BaseCategory,
  type FunctionKey,
  type Role,
} from "../api";
import { useT } from "@/shared/i18n";
import { toast, toastFirstError } from "@/shared/lib/toast-store";
import {
  ROLE_NAME_MAX,
  checkCellToggle,
  roleKeyFor,
  validateRoleName,
} from "../model/guards";
import {
  Button,
  Card,
  ConfirmDialog,
  DataTable,
  NoticeTile,
  PageHeader,
  SegmentedTabs,
  SelectField,
  TextField,
  type DataTableColumn,
} from "@/shared/ui";

export function AdminPermissionMatrixView() {
  const t = useT("adminPermissionMatrix");
  const [page, setPage] = useState(fetchPermissionMatrix);
  const [activeRoleKey, setActiveRoleKey] = useState("INSTRUCTOR");
  const [addingRole, setAddingRole] = useState(false);
  const [newRoleName, setNewRoleName] = useState("");
  const [newRoleCategory, setNewRoleCategory] =
    useState<BaseCategory>("STUDENT");
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [nameTouched, setNameTouched] = useState(false);
  // A cell waiting on the "you are removing your own role's right" confirmation.
  const [pendingSelf, setPendingSelf] = useState<{
    functionKey: FunctionKey;
    action: ActionKey;
  } | null>(null);

  const activeRole: Role =
    page.roles.find((role) => role.key === activeRoleKey) ?? page.roles[0]!;
  // STUDENT's rights live outside the matrix (F1-05), so its cells are shown for reference only.
  const readOnly = activeRole.key === "STUDENT";

  function requestToggle(functionKey: FunctionKey, action: ActionKey) {
    if (readOnly) return;
    const granted = grantsFor(page.permissions, activeRole.key, functionKey)[
      action
    ];
    const check = checkCellToggle(
      page,
      activeRole.key,
      functionKey,
      action,
      granted,
    );
    if (check.blockedLastMatrixAdmin) toast.warning(t("lastMatrixAdminBody"));
    else if (check.revokesOwnRole) setPendingSelf({ functionKey, action });
    else applyToggle(functionKey, action);
  }

  function applyToggle(functionKey: FunctionKey, action: ActionKey) {
    setPage((previous) => {
      const current = grantsFor(
        previous.permissions,
        activeRole.key,
        functionKey,
      );
      return {
        ...previous,
        permissions: {
          ...previous.permissions,
          [activeRole.key]: {
            ...previous.permissions[activeRole.key],
            [functionKey]: { ...current, [action]: !current[action] },
          },
        },
      };
    });
  }

  const nameError = validateRoleName(newRoleName, page.roles);

  function createRole() {
    setNameTouched(true);
    if (nameError) {
      toastFirstError([
        t(`roleNameError.${nameError}`, { max: ROLE_NAME_MAX }),
      ]);
      return;
    }
    const name = newRoleName.trim();
    const key = roleKeyFor(name);
    setPage((previous) => ({
      ...previous,
      // Owner 2026-10-08 (ADM0202 Q6, hướng 1): the form picks the category, STUDENT by default. The
      // cells stay editable for every custom role because `readOnly` keys off the system role STUDENT,
      // not off this category; BD Sheet 6 says otherwise, which is the part of Q6 still open.
      roles: [
        ...previous.roles,
        {
          key,
          label: name,
          system: false,
          baseCategory: newRoleCategory,
          userCount: 0,
        },
      ],
      permissions: { ...previous.permissions, [key]: {} },
    }));
    setActiveRoleKey(key);
    setAddingRole(false);
    setNewRoleName("");
    setNewRoleCategory("STUDENT");
    setNameTouched(false);
    toast.success(t("createRoleDone", { role: name }));
  }

  function deleteActiveRole() {
    setPage((previous) => {
      const permissions = { ...previous.permissions };
      delete permissions[activeRole.key];
      return {
        ...previous,
        roles: previous.roles.filter((role) => role.key !== activeRole.key),
        permissions,
      };
    });
    setActiveRoleKey("INSTRUCTOR");
    toast.success(t("deleteRoleDone", { role: activeRole.label }));
    setConfirmingDelete(false);
  }

  const columns: DataTableColumn<FunctionKey>[] = [
    {
      key: "function",
      header: t("columnFunction"),
      render: (functionKey) => (
        <span className="block">
          <span className="block text-[13px] font-semibold">
            {t(`function.${functionKey}`)}
          </span>
          <span className="mt-0.5 block font-mono text-[11px] text-[var(--color-text-subtle)]">
            {functionKey}
          </span>
        </span>
      ),
    },
    ...ACTION_KEYS.map<DataTableColumn<FunctionKey>>((action) => ({
      key: action,
      header: t(`action.${action}`),
      width: "84px",
      align: "center",
      render: (functionKey) => {
        const granted = grantsFor(
          page.permissions,
          activeRole.key,
          functionKey,
        )[action];
        return (
          <input
            type="checkbox"
            checked={granted}
            disabled={readOnly}
            onChange={() => requestToggle(functionKey, action)}
            aria-label={t("cellLabel", {
              action: t(`action.${action}`),
              function: t(`function.${functionKey}`),
              role: activeRole.label,
            })}
            className="h-[22px] w-[22px] cursor-pointer rounded-[7px] accent-[var(--color-admin-teal)] disabled:cursor-not-allowed disabled:opacity-50"
          />
        );
      },
    })),
  ];

  return (
    <div>
      <PageHeader title={t("title")} description={t("subtitle")} />

      <NoticeTile tone="info" title={t("scopeNoticeTitle")} className="mb-4">
        {t("scopeNoticeBody")}
      </NoticeTile>

      <Card className="min-w-0 px-[18px] py-4">
        <div className="mb-4 flex flex-wrap items-center gap-2">
          <SegmentedTabs
            label={t("roleSwitcherLabel")}
            value={activeRoleKey}
            onValueChange={setActiveRoleKey}
            options={page.roles.map((role) => ({
              value: role.key,
              label: role.label,
            }))}
          />

          {addingRole ? (
            <span className="flex items-center gap-2">
              <TextField
                label={t("newRoleLabel")}
                hideLabel
                placeholder={t("newRolePlaceholder")}
                value={newRoleName}
                onChange={(event) => setNewRoleName(event.target.value)}
                maxLength={ROLE_NAME_MAX + 10}
                invalid={nameTouched && Boolean(nameError)}
                wrapperClassName="w-[170px]"
                className="h-8"
              />
              <SelectField
                label={t("newRoleCategoryLabel")}
                hideLabel
                value={newRoleCategory}
                onChange={(event) =>
                  setNewRoleCategory(event.target.value as BaseCategory)
                }
                options={(["STUDENT", "INSTRUCTOR", "ADMIN"] as const).map(
                  (value) => ({
                    value,
                    label: t(`baseCategory.${value}`),
                  }),
                )}
                wrapperClassName="w-[190px]"
                className="h-8"
              />
              <Button
                variant="cta"
                size="sm"
                onClick={createRole}
                disabled={!newRoleName.trim()}
              >
                {t("createRole")}
              </Button>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setAddingRole(false);
                  setNewRoleName("");
                  setNewRoleCategory("STUDENT");
                  setNameTouched(false);
                }}
              >
                {t("cancel")}
              </Button>
            </span>
          ) : (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setAddingRole(true)}
              className="border border-dashed border-[var(--color-border)]"
            >
              {t("addRole")}
            </Button>
          )}

          {activeRole.system ? (
            <span className="ml-auto text-xs text-[var(--color-text-subtle)]">
              {t("systemRoleNote")}
            </span>
          ) : (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                // Q2: a role that accounts still hold is blocked, not silently reassigned.
                if (activeRole.userCount > 0) {
                  toast.warning(
                    t("deleteBlockedBody", {
                      role: activeRole.label,
                      count: activeRole.userCount,
                    }),
                  );
                } else {
                  setConfirmingDelete(true);
                }
              }}
              className="ml-auto border border-[var(--color-admin-negative)] text-[var(--color-admin-negative)]"
            >
              {t("deleteRole")}
            </Button>
          )}
        </div>

        {readOnly ? (
          <NoticeTile
            tone="info"
            title={t("studentNoticeTitle")}
            className="mb-4"
          >
            {t("studentNoticeBody")}
          </NoticeTile>
        ) : null}

        <DataTable
          caption={t("tableCaption", { role: activeRole.label })}
          columns={columns}
          rows={FUNCTION_KEYS}
          rowKey={(functionKey) => functionKey}
          emptyMessage={t("emptyFunctions")}
          minWidth={640}
        />
      </Card>

      <ConfirmDialog
        open={pendingSelf !== null}
        onClose={() => setPendingSelf(null)}
        onConfirm={() => {
          if (pendingSelf)
            applyToggle(pendingSelf.functionKey, pendingSelf.action);
          setPendingSelf(null);
        }}
        title={t("selfRevokeTitle")}
        confirmLabel={t("selfRevokeConfirm")}
        cancelLabel={t("cancel")}
        destructive
      >
        {t("selfRevokeBody")}
      </ConfirmDialog>

      <ConfirmDialog
        open={confirmingDelete}
        onClose={() => setConfirmingDelete(false)}
        onConfirm={deleteActiveRole}
        title={t("confirmDeleteTitle", { role: activeRole.label })}
        confirmLabel={t("deleteRole")}
        cancelLabel={t("cancel")}
        destructive
      >
        {t("confirmDeleteBody")}
      </ConfirmDialog>
    </div>
  );
}
