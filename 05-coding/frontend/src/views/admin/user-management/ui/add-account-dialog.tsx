// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// "Thêm tài khoản" (ADM0201 Q1, DEC-2026-1001-admin-configurable-settings): ADMIN creates an
// INSTRUCTOR or STUDENT account by hand; the temporary password goes out by email. No endpoint yet,
// so the screen adds the row locally and toasts that the email was "sent".
"use client";

import { useState } from "react";
import { useT } from "@/shared/i18n";
import { toast, toastFirstError } from "@/shared/lib/toast-store";
import { Button, Modal, SelectField, TextField } from "@/shared/ui";
import type { AdminUser, AdminUserRole } from "../model/types";

type CreatableRole = Exclude<AdminUserRole, "admin">;

type Props = {
  open: boolean;
  onClose: () => void;
  existingEmails: ReadonlySet<string>;
  onCreate: (user: AdminUser) => void;
};

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function AddAccountDialog({ open, onClose, existingEmails, onCreate }: Props) {
  const t = useT("adminUserManagement");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [role, setRole] = useState<CreatableRole>("student");
  const [touched, setTouched] = useState(false);

  const normalized = email.trim().toLowerCase();
  const nameError = name.trim() ? undefined : t("addDialog.errorName");
  const emailError = !EMAIL_PATTERN.test(normalized)
    ? t("addDialog.errorEmail")
    : existingEmails.has(normalized)
      ? t("addDialog.errorDuplicate")
      : undefined;

  function close() {
    setName("");
    setEmail("");
    setRole("student");
    setTouched(false);
    onClose();
  }

  function submit() {
    setTouched(true);
    if (nameError || emailError) {
      toastFirstError([nameError, emailError]);
      return;
    }
    onCreate({
      name: name.trim(),
      email: normalized,
      role,
      solvedCount: 0,
      submissionCount: 0,
      lastActiveLabel: "-",
      status: "pending",
    });
    toast.success(t("addDialog.createdBody", { email: normalized }));
    close();
  }

  return (
    <Modal
      open={open}
      onClose={close}
      title={t("addDialog.title")}
      footer={
        <>
          <Button variant="ghost" size="sm" className="border border-[var(--color-border)]" onClick={close}>
            {t("cancel")}
          </Button>
          <Button variant="cta" size="sm" onClick={submit}>
            {t("addDialog.submit")}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-3">
        <TextField
          label={t("addDialog.nameLabel")}
          value={name}
          onChange={(event) => setName(event.target.value)}
          invalid={touched && Boolean(nameError)}
        />
        <TextField
          label={t("addDialog.emailLabel")}
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          invalid={touched && Boolean(emailError)}
        />
        <SelectField
          label={t("addDialog.roleLabel")}
          value={role}
          onChange={(event) => setRole(event.target.value as CreatableRole)}
          options={[
            { value: "student", label: t("role.student") },
            { value: "instructor", label: t("role.instructor") },
          ]}
        />
        <p className="text-[12.5px] text-[var(--color-text-muted)]">{t("addDialog.note")}</p>
      </div>
    </Modal>
  );
}
