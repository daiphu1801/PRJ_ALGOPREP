// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// "Đổi vai trò" for ONE account (F1-13, ADM0201 Q7: moved out of the bulk bar because forcing a whole
// selection onto one target role has no undo). The rules live in `checkRoleChange`; this dialog only
// shows the self-demotion warning and disables submit while nothing has changed.
"use client";

import { useState } from "react";
import { useT } from "@/shared/i18n";
import { Button, FilterMenu, Modal } from "@/shared/ui";
import type { AdminUser, AdminUserRole } from "../model/types";

type Props = {
  /** The account being edited; `null` keeps the dialog closed. */
  user: AdminUser | null;
  /** The acting admin is editing their own row: demoting is allowed but says so out loud. */
  isSelf: boolean;
  onClose: () => void;
  onConfirm: (user: AdminUser, role: AdminUserRole) => void;
};

export function ChangeRoleDialog({ user, isSelf, onClose, onConfirm }: Props) {
  const t = useT("adminUserManagement");
  // Keyed by email so reopening for another account starts from that account's role.
  const [picked, setPicked] = useState<{
    email: string;
    role: AdminUserRole;
  } | null>(null);
  const role =
    user && picked?.email === user.email
      ? picked.role
      : (user?.role ?? "student");
  const unchanged = !user || role === user.role;

  function close() {
    setPicked(null);
    onClose();
  }

  return (
    <Modal
      open={user !== null}
      onClose={close}
      title={t("roleDialog.title", { name: user?.name ?? "" })}
      footer={
        <>
          <Button
            variant="ghost"
            size="sm"
            className="border border-[var(--color-border)]"
            onClick={close}
          >
            {t("cancel")}
          </Button>
          <Button
            variant="cta"
            size="sm"
            disabled={unchanged}
            onClick={() => {
              if (user) onConfirm(user, role);
              close();
            }}
          >
            {t("roleDialog.submit")}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-3">
        <div className="flex min-w-0 flex-col gap-1.5">
          <span className="text-[13px] font-medium">
            {t("roleDialog.label")}
          </span>
          <FilterMenu<AdminUserRole>
            label={t("roleDialog.label")}
            compact
            className="w-full [&>button]:h-10 [&>button]:w-full [&>button]:justify-between [&>button]:bg-[var(--color-surface)] [&>button]:px-3 [&>button]:text-sm [&>button]:text-[var(--color-text)] [&>[role=listbox]]:w-full [&>[role=listbox]]:max-w-full"
            value={role}
            onValueChange={(nextRole) => {
              if (user) setPicked({ email: user.email, role: nextRole });
            }}
            options={[
              { value: "student", label: t("role.student") },
              { value: "instructor", label: t("role.instructor") },
              { value: "admin", label: t("role.admin") },
            ]}
          />
        </div>
        <p className="text-[12.5px] text-[var(--color-text-muted)]">
          {t("roleDialog.note")}
        </p>
        {isSelf && role !== "admin" ? (
          <p className="text-[12.5px] font-semibold text-[var(--color-admin-warn)]">
            {t("roleDialog.selfWarning")}
          </p>
        ) : null}
      </div>
    </Modal>
  );
}
