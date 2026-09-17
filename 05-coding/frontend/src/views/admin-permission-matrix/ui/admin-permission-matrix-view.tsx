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
// 2. Toggling a cell applies immediately rather than waiting for the header button. F1-10 says the
//    change "có hiệu lực ngay", which the mockup's own subtitle repeats, so a pending-save model
//    would contradict the requirement. The header button confirms rather than commits — whether it
//    should exist at all is an open question carried into the phase report.
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
  type FunctionKey,
  type Role,
} from "@/entities/permission-matrix";
import { useT } from "@/shared/i18n";
import {
  Button,
  Card,
  ConfirmDialog,
  DataTable,
  NoticeTile,
  PageHeader,
  SegmentedTabs,
  TextField,
  type DataTableColumn,
} from "@/shared/ui";

export function AdminPermissionMatrixView() {
  const t = useT("adminPermissionMatrix");
  const [page, setPage] = useState(fetchPermissionMatrix);
  const [activeRoleKey, setActiveRoleKey] = useState("INSTRUCTOR");
  const [addingRole, setAddingRole] = useState(false);
  const [newRoleName, setNewRoleName] = useState("");
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [justSaved, setJustSaved] = useState(false);

  const activeRole: Role =
    page.roles.find((role) => role.key === activeRoleKey) ?? page.roles[0]!;
  // STUDENT's rights live outside the matrix (F1-05), so its cells are shown for reference only.
  const readOnly = activeRole.key === "STUDENT";

  function toggleCell(functionKey: FunctionKey, action: ActionKey) {
    if (readOnly) return;
    setPage((previous) => {
      const current = grantsFor(previous.permissions, activeRole.key, functionKey);
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
    setJustSaved(false);
  }

  function createRole() {
    const name = newRoleName.trim();
    if (!name) return;
    const key = `role_${name.toLowerCase().replace(/\s+/g, "_")}`;
    setPage((previous) => ({
      roles: [...previous.roles, { key, label: name, system: false }],
      permissions: { ...previous.permissions, [key]: {} },
    }));
    setActiveRoleKey(key);
    setAddingRole(false);
    setNewRoleName("");
  }

  function deleteActiveRole() {
    setPage((previous) => {
      const permissions = { ...previous.permissions };
      delete permissions[activeRole.key];
      return {
        roles: previous.roles.filter((role) => role.key !== activeRole.key),
        permissions,
      };
    });
    setActiveRoleKey("INSTRUCTOR");
    setConfirmingDelete(false);
  }

  const columns: DataTableColumn<FunctionKey>[] = [
    {
      key: "function",
      header: t("columnFunction"),
      render: (functionKey) => (
        <span className="block">
          <span className="block text-[13px] font-semibold">{t(`function.${functionKey}`)}</span>
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
        const granted = grantsFor(page.permissions, activeRole.key, functionKey)[action];
        return (
          <input
            type="checkbox"
            checked={granted}
            disabled={readOnly}
            onChange={() => toggleCell(functionKey, action)}
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
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        actions={
          <Button variant="cta" size="sm" onClick={() => setJustSaved(true)}>
            {justSaved ? t("saved") : t("save")}
          </Button>
        }
      />

      <NoticeTile tone="info" title={t("scopeNoticeTitle")} className="mb-4">
        {t("scopeNoticeBody")}
      </NoticeTile>

      <Card className="min-w-0 px-[18px] py-4">
        <div className="mb-4 flex flex-wrap items-center gap-2">
          <SegmentedTabs
            label={t("roleSwitcherLabel")}
            value={activeRoleKey}
            onValueChange={setActiveRoleKey}
            options={page.roles.map((role) => ({ value: role.key, label: role.label }))}
          />

          {addingRole ? (
            <span className="flex items-center gap-2">
              <TextField
                label={t("newRoleLabel")}
                hideLabel
                placeholder={t("newRolePlaceholder")}
                value={newRoleName}
                onChange={(event) => setNewRoleName(event.target.value)}
                wrapperClassName="w-[170px]"
                className="h-8"
              />
              <Button variant="cta" size="sm" onClick={createRole} disabled={!newRoleName.trim()}>
                {t("createRole")}
              </Button>
              <Button variant="ghost" size="sm" onClick={() => setAddingRole(false)}>
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
              onClick={() => setConfirmingDelete(true)}
              className="ml-auto border border-[var(--color-admin-negative)] text-[var(--color-admin-negative)]"
            >
              {t("deleteRole")}
            </Button>
          )}
        </div>

        {readOnly ? (
          <NoticeTile tone="info" title={t("studentNoticeTitle")} className="mb-4">
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
